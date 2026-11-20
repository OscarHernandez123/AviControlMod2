package com.avicontrol.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.function.BooleanSupplier;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Prueba optativa: crea un galpón y lote dedicados en los servicios de demostración ya levantados.
 */
@EnabledIfEnvironmentVariable(named = "AVICONTROL_RUN_KAFKA_LIVE_TESTS", matches = "true")
class Modulo1KafkaLiveTest {
  private final HttpClient http = HttpClient.newHttpClient();
  private final JsonMapper json = JsonMapper.builder().build();
  private final String m1 =
      System.getenv().getOrDefault("AVICONTROL_LIVE_MODULO1_URL", "http://localhost:8080");
  private final String m2 =
      System.getenv().getOrDefault("AVICONTROL_LIVE_MODULO2_URL", "http://localhost:8081");
  private final String base = "/api/integracion/modulo1/eventos";
  private final String auth =
      "Basic "
          + Base64.getEncoder()
              .encodeToString("admin:admin123".getBytes(java.nio.charset.StandardCharsets.UTF_8));

  @Test
  void publicaTresTopicosYElModulo1AplicaLosEfectosUnaSolaVez() throws Exception {
    String nombre = "Kafka " + UUID.randomUUID().toString().substring(0, 8);
    String galpon =
        call(
                m1,
                "POST",
                "/api/v1/galpones",
                "{\"nombre\":\"" + nombre + "\",\"capacidad\":100}",
                201)
            .get("id")
            .asString();
    String lote =
        call(
                m1,
                "POST",
                "/api/v1/lotes",
                "{\"galponId\":\""
                    + galpon
                    + "\",\"nombre\":\""
                    + nombre
                    + "\",\"fechaIngreso\":\""
                    + LocalDate.now(ZoneId.of("America/Bogota"))
                    + "\",\"poblacionInicial\":100,\"costoTotal\":10000}",
                201)
            .get("id")
            .asString();
    String mortalidad = UUID.randomUUID().toString(),
        aislar = UUID.randomUUID().toString(),
        reanudar = UUID.randomUUID().toString(),
        vaciar = UUID.randomUUID().toString();
    var ids = Set.of(mortalidad, aislar, reanudar, vaciar);
    var config = new HashMap<String, Object>();
    config.put(
        "bootstrap.servers",
        System.getenv().getOrDefault("SPRING_KAFKA_BOOTSTRAP_SERVERS", "localhost:19092"));
    config.put("group.id", "verificacion-" + UUID.randomUUID());
    config.put("auto.offset.reset", "earliest");
    config.put("enable.auto.commit", false);
    var mensajes = new HashMap<String, JsonNode>();
    try (var consumer =
        new KafkaConsumer<String, String>(
            config, new StringDeserializer(), new StringDeserializer())) {
      consumer.subscribe(
          List.of(
              "avicontrol.mortalidad",
              "avicontrol.alerta-sanitaria",
              "avicontrol.vaciado-sanitario"));
      String payload = payload(mortalidad, galpon, lote, false) + ",\"cantidadMuertos\":2}";
      call(m2, "POST", base + "/mortalidad", payload, 202);
      await(() -> detalle(galpon).path("loteActivo").path("poblacionActual").asInt() == 98);
      call(m2, "POST", base + "/mortalidad", payload, 202);
      assertEquals(98, detalle(galpon).path("loteActivo").path("poblacionActual").asInt());
      call(
          m2,
          "POST",
          base + "/mortalidad",
          payload.replace("\"cantidadMuertos\":2", "\"cantidadMuertos\":3"),
          409);
      call(
          m2,
          "POST",
          base + "/mortalidad",
          payload
              .replace(mortalidad, UUID.randomUUID().toString())
              .replace("\"cantidadMuertos\":2", "\"cantidadMuertos\":2.0"),
          400);
      call(
          m2,
          "POST",
          base + "/mortalidad",
          payload
              .replace(mortalidad, UUID.randomUUID().toString())
              .replace("\"cantidadMuertos\":2", "\"cantidadMuertos\":\"2\""),
          400);
      call(
          m2,
          "POST",
          base + "/alerta-sanitaria",
          payload(aislar, galpon, lote, true)
              + ",\"accion\":\"AISLAMIENTO\",\"tipoEnfermedad\":\"Sospecha\",\"descripcion\":\"Prueba Kafka\"}",
          202);
      await(() -> detalle(galpon).path("estado").asString().equals("AISLAMIENTO"));
      call(
          m2,
          "POST",
          base + "/alerta-sanitaria",
          payload(reanudar, galpon, lote, true) + ",\"accion\":\"REANUDACION\"}",
          202);
      await(() -> detalle(galpon).path("estado").asString().equals("PRODUCTIVO"));
      call(
          m1,
          "PATCH",
          "/api/v1/galpones/" + galpon + "/estado",
          "{\"estadoEsperado\":\"PRODUCTIVO\",\"estadoDestino\":\"EN_COSECHA\"}",
          200);
      call(
          m2, "POST", base + "/vaciado-sanitario", payload(vaciar, galpon, lote, false) + "}", 202);
      await(() -> detalle(galpon).path("estado").asString().equals("VACIADO_SANITARIO"));
      assertTrue(detalle(galpon).path("loteActivo").isMissingNode());
      long limite = System.nanoTime() + Duration.ofSeconds(20).toNanos();
      while (mensajes.size() < 4 && System.nanoTime() < limite) {
        for (var r : consumer.poll(Duration.ofMillis(500))) {
          var evento = json.readTree(r.value());
          String id = evento.path("payload").path("alertaId").asString();
          if (!ids.contains(id)) continue;
          assertEquals(galpon, r.key());
          assertEquals(1, evento.path("eventVersion").asInt());
          assertEquals("modulo-2", evento.path("producer").asString());
          assertEquals(galpon, evento.path("aggregateId").asString());
          assertFalse(r.value().contains(":null"));
          OffsetDateTime.parse(evento.path("occurredAt").asString());
          OffsetDateTime.parse(evento.path("publishedAt").asString());
          assertEquals(
              switch (r.topic()) {
                case "avicontrol.mortalidad" -> "MORTALIDAD_REGISTRADA";
                case "avicontrol.alerta-sanitaria" -> "ALERTA_SANITARIA_EMITIDA";
                default -> "VACIADO_SANITARIO_SOLICITADO";
              },
              evento.path("eventType").asString());
          mensajes.put(id, evento);
        }
      }
    }
    assertEquals(4, mensajes.size());
    for (String id : ids)
      assertEquals(
          "PUBLICADO", call(m2, "GET", base + "/" + id, null, 200).path("estado").asString());
    java.nio.file.Files.writeString(
        java.nio.file.Path.of("build/integracion-rest/eventos-kafka.json"),
        json.writerWithDefaultPrettyPrinter().writeValueAsString(mensajes));
  }

  private String payload(String id, String galpon, String lote, boolean sanitaria) {
    return "{\"alertaId\":\""
        + id
        + "\",\"galponId\":\""
        + galpon
        + "\",\"loteId\":\""
        + lote
        + "\",\""
        + (sanitaria ? "fechaHora" : "fechaHoraEvento")
        + "\":\""
        + OffsetDateTime.now(ZoneId.of("America/Bogota")).minusSeconds(1)
        + "\"";
  }

  private JsonNode detalle(String id) {
    try {
      return call(m1, "GET", "/api/v1/galpones/" + id, null, 200);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void await(BooleanSupplier condition) throws InterruptedException {
    for (int i = 0; i < 60; i++) {
      if (condition.getAsBoolean()) return;
      Thread.sleep(250);
    }
    fail("No se aplicó el efecto en Módulo 1");
  }

  private JsonNode call(String server, String method, String path, String body, int status)
      throws Exception {
    var req =
        HttpRequest.newBuilder(URI.create(server + path))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json");
    if (server.equals(m2)) req.header("Authorization", auth);
    var response =
        http.send(
            req.method(
                    method,
                    body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertEquals(status, response.statusCode(), method + " " + path + " " + response.body());
    return json.readTree(response.body());
  }
}
