package com.avicontrol.infrastructure.adapter.in.rest.sacrificio.productivo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avicontrol.AviControlModulo2Application;
import com.avicontrol.application.sacrificio.productivo.CorregirResultadoSacrificioUseCase;
import com.avicontrol.application.sacrificio.productivo.RegistrarResultadoSacrificioUseCase;
import com.avicontrol.infrastructure.adapter.in.event.sacrificio.productivo.Modulo3LiquidacionLoteEventListener;
import com.avicontrol.infrastructure.adapter.in.event.sacrificio.productivo.ResultadoSacrificioUtilizadoV1;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = AviControlModulo2Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResultadosSacrificioControllerTest {
  @DynamicPropertySource
  static void baseAislada(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.datasource.url",
        () ->
            "jdbc:h2:mem:resultados_sacrificio_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
  }

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired Clock clock;
  @Autowired RegistrarResultadoSacrificioUseCase registro;
  @Autowired CorregirResultadoSacrificioUseCase correccion;
  @Autowired Modulo3LiquidacionLoteEventListener eventos;
  private final ObjectMapper json = new ObjectMapper();

  @BeforeEach
  void limpiar() {
    jdbc.update("DELETE FROM confirmaciones_uso_resultado_sacrificio");
    jdbc.update("DELETE FROM resultados_sacrificio_productivo");
    jdbc.update("DELETE FROM ordenes_sacrificio_productivo");
  }

  @Test
  void registraCorrigeConsultaYBloqueaTrasUtilizacion() throws Exception {
    UUID lote = UUID.randomUUID();
    UUID orden = orden("EJECUTADA", lote);
    String body =
        mvc.perform(
                post("/api/ordenes-sacrificio/{id}/resultados", orden)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(datos(7800, "18720.50"))
                    .with(admin()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.estado").value("DISPONIBLE"))
            .andExpect(jsonPath("$.cantidadFinalPollos").value(7800))
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID resultado = UUID.fromString(json.readTree(body).get("id").asText());
    mvc.perform(
            put("/api/ordenes-sacrificio/{id}/resultados", orden)
                .contentType(MediaType.APPLICATION_JSON)
                .content(datos(7790, "18700.00"))
                .with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pesoTotalKg").value(18700.0));
    mvc.perform(get("/api/ordenes-sacrificio/{id}/resultados", orden).with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.loteId").value(lote.toString()));
    var evento =
        new ResultadoSacrificioUtilizadoV1(
            UUID.randomUUID(),
            "ResultadoSacrificioUtilizado",
            1,
            Instant.now(clock),
            Instant.now(clock),
            "modulo3",
            UUID.randomUUID(),
            UUID.randomUUID(),
            "ResultadoSacrificio",
            resultado,
            new ResultadoSacrificioUtilizadoV1.Payload(resultado, lote));
    eventos.procesar(evento);
    eventos.procesarKafka(
        """
        {
          "eventId": "%s",
          "eventType": "ResultadoSacrificioUtilizado",
          "eventVersion": 1,
          "occurredAt": "%s",
          "publishedAt": "%s",
          "producer": "modulo3",
          "correlationId": "%s",
          "causationId": "%s",
          "aggregateType": "ResultadoSacrificio",
          "aggregateId": "%s",
          "payload": {
            "resultadoId": "%s",
            "loteId": "%s"
          }
        }
        """
            .formatted(
                evento.eventId(),
                evento.occurredAt(),
                evento.publishedAt(),
                evento.correlationId(),
                evento.causationId(),
                resultado,
                resultado,
                lote));
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM confirmaciones_uso_resultado_sacrificio WHERE evento_id=?",
            Integer.class,
            evento.eventId()));
    mvc.perform(
            put("/api/ordenes-sacrificio/{id}/resultados", orden)
                .contentType(MediaType.APPLICATION_JSON)
                .content(datos(7780, "18600.00"))
                .with(admin()))
        .andExpect(status().isConflict());
    mvc.perform(get("/api/ordenes-sacrificio/{id}/resultados", orden).with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("UTILIZADO"))
        .andExpect(jsonPath("$.cantidadFinalPollos").value(7790));
    mvc.perform(
            get("/api/ordenes-sacrificio/{id}/resultados", orden)
                .with(SecurityMockMvcRequestPostProcessors.user("modulo3").roles("MODULO3")))
        .andExpect(status().isOk());
    mvc.perform(
            post("/api/ordenes-sacrificio/{id}/resultados", orden)
                .contentType(MediaType.APPLICATION_JSON)
                .content(datos(7800, "18720.50"))
                .with(admin()))
        .andExpect(status().isConflict());
  }

  @Test
  void rechazaOrdenNoEjecutadaValoresInvalidosYAcceso() throws Exception {
    UUID orden = orden("PROGRAMADA", UUID.randomUUID());
    mvc.perform(
            post("/api/ordenes-sacrificio/{id}/resultados", orden)
                .contentType(MediaType.APPLICATION_JSON)
                .content(datos(7800, "18720.50"))
                .with(admin()))
        .andExpect(status().isConflict());
    mvc.perform(
            post("/api/ordenes-sacrificio/{id}/resultados", orden)
                .contentType(MediaType.APPLICATION_JSON)
                .content(datos(0, "18720.50"))
                .with(admin()))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/ordenes-sacrificio/{id}/resultados", orden)
                .contentType(MediaType.APPLICATION_JSON)
                .content(datos(7800, "18720.501"))
                .with(admin()))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/ordenes-sacrificio/{id}/resultados", UUID.randomUUID()).with(admin()))
        .andExpect(status().isNotFound());
    mvc.perform(post("/api/ordenes-sacrificio/{id}/resultados", orden))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/ordenes-sacrificio/{id}/resultados", orden)
                .contentType(MediaType.APPLICATION_JSON)
                .content(datos(7800, "18720.50"))
                .with(SecurityMockMvcRequestPostProcessors.user("worker").roles("TRABAJADOR")))
        .andExpect(status().isForbidden());
  }

  @Test
  void dosRegistrosConcurrentesSoloCreanUno() throws Exception {
    UUID orden = orden("EJECUTADA", UUID.randomUUID());
    var inicio = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      var uno = pool.submit(() -> intentarRegistro(orden, inicio));
      var dos = pool.submit(() -> intentarRegistro(orden, inicio));
      inicio.countDown();
      int creados =
          (uno.get(10, TimeUnit.SECONDS) ? 1 : 0) + (dos.get(10, TimeUnit.SECONDS) ? 1 : 0);
      assertEquals(1, creados);
    }
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM resultados_sacrificio_productivo WHERE orden_id=?",
            Integer.class,
            orden));
  }

  @Test
  void correccionYConfirmacionConcurrentesNuncaSobrescribenResultadoUtilizado() throws Exception {
    UUID lote = UUID.randomUUID();
    UUID orden = orden("EJECUTADA", lote);
    UUID resultado = registro.registrar(orden, 7800, new BigDecimal("18720.50")).id();
    var evento =
        new ResultadoSacrificioUtilizadoV1(
            UUID.randomUUID(),
            "ResultadoSacrificioUtilizado",
            1,
            Instant.now(clock),
            Instant.now(clock),
            "modulo3",
            UUID.randomUUID(),
            UUID.randomUUID(),
            "ResultadoSacrificio",
            resultado,
            new ResultadoSacrificioUtilizadoV1.Payload(resultado, lote));
    var inicio = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      var editar =
          pool.submit(
              () -> {
                inicio.await();
                try {
                  correccion.corregir(orden, 7790, new BigDecimal("18700.00"));
                  return true;
                } catch (RuntimeException exception) {
                  return false;
                }
              });
      var utilizar =
          pool.submit(
              () -> {
                inicio.await();
                eventos.procesar(evento);
                return true;
              });
      inicio.countDown();
      boolean corrigio = editar.get(10, TimeUnit.SECONDS);
      utilizar.get(10, TimeUnit.SECONDS);
      assertEquals(
          "UTILIZADO",
          jdbc.queryForObject(
              "SELECT estado FROM resultados_sacrificio_productivo WHERE id=?",
              String.class,
              resultado));
      assertEquals(
          corrigio ? 7790 : 7800,
          jdbc.queryForObject(
              "SELECT cantidad_final_pollos FROM resultados_sacrificio_productivo WHERE id=?",
              Integer.class,
              resultado));
    }
  }

  private boolean intentarRegistro(UUID orden, CountDownLatch inicio) throws InterruptedException {
    inicio.await();
    try {
      registro.registrar(orden, 7800, new BigDecimal("18720.50"));
      return true;
    } catch (RuntimeException exception) {
      return false;
    }
  }

  private UUID orden(String estado, UUID lote) {
    UUID id = UUID.randomUUID();
    Instant ahora = Instant.now(clock);
    jdbc.update(
        "INSERT INTO ordenes_sacrificio_productivo (id,galpon_id,lote_id,estado,fecha_hora_programada,fecha_creacion,fecha_ejecucion,version) VALUES (?,?,?,?,?,?,?,?)",
        id,
        UUID.randomUUID(),
        lote,
        estado,
        java.sql.Timestamp.from(ahora.minusSeconds(10)),
        java.sql.Timestamp.from(ahora.minusSeconds(100)),
        estado.equals("EJECUTADA") ? java.sql.Timestamp.from(ahora) : null,
        estado.equals("EJECUTADA") ? 1 : 0);
    return id;
  }

  private String datos(int cantidad, String peso) {
    return "{\"cantidadFinalPollos\":" + cantidad + ",\"pesoTotalKg\":" + peso + "}";
  }

  private SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor admin() {
    return SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR");
  }
}
