package com.avicontrol.infrastructure.adapter.out.integration.modulo1;

import static org.junit.jupiter.api.Assertions.*;

import com.avicontrol.domain.exception.galpon.InformacionGalponNoDisponibleException;
import com.avicontrol.domain.exception.sanitario.aislamiento.Modulo1NoDisponibleException;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Modulo1RestClientTest {
  private static final UUID ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
  private HttpServer server;
  private Modulo1RestClient client;
  private int status;
  private String body;
  private final ArrayList<String> requests = new ArrayList<>();

  @BeforeEach
  void iniciar() throws Exception {
    status = 200;
    body = fixture("galpon-detalle-response.json");
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/api/v1/galpones",
        exchange -> {
          requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI());
          byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(status, bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });
    server.start();
    client =
        new Modulo1RestClient(
            URI.create("http://127.0.0.1:" + server.getAddress().getPort()),
            Duration.ofSeconds(1),
            Duration.ofSeconds(2));
  }

  @AfterEach
  void detener() {
    server.stop(0);
  }

  @Test
  void mapeaElContratoRealEnLosCuatroPuertos() {
    var galpon = new GalponRestQueryAdapter(client).consultar(ID).orElseThrow();
    assertEquals(1500, galpon.aforoMaximo());
    assertEquals("PRODUCTIVO", galpon.estado());
    var lote = new LoteRestQueryAdapter(client).consultarActual(ID).orElseThrow();
    assertEquals(975, lote.poblacionActual());
    assertEquals(LocalDate.of(2026, 9, 15), lote.fechaIngreso());
    assertEquals(ID, lote.galponId());
    var sanitario = new GalponAislamientoRestQueryAdapter(client).consultar(ID).orElseThrow();
    assertEquals(lote.id(), sanitario.loteId());
    var medicacion = new GalponMedicacionRestQueryAdapter(client).consultar(ID).orElseThrow();
    assertEquals("Lote A1", medicacion.loteCodigo());
    assertEquals(975, medicacion.poblacionViva());
    assertTrue(requests.stream().allMatch(r -> r.equals("GET /api/v1/galpones/" + ID)));
  }

  @Test
  void ausenciaDeLoteNoSeConfundeConGalponInexistente() throws Exception {
    body =
        fixture("galpon-detalle-sin-lote-response.json")
            .replace("6ba7b810-9dad-41d1-80b4-00c04fd430c8", ID.toString());
    assertTrue(new GalponRestQueryAdapter(client).consultar(ID).isPresent());
    assertTrue(new LoteRestQueryAdapter(client).consultarActual(ID).isEmpty());
    assertNull(new GalponAislamientoRestQueryAdapter(client).consultar(ID).orElseThrow().loteId());
    assertNull(new GalponMedicacionRestQueryAdapter(client).consultar(ID).orElseThrow().loteId());
  }

  @Test
  void solo404RepresentaAusencia() {
    status = 404;
    body = "{\"code\":\"GALPON_NO_ENCONTRADO\"}";
    assertTrue(client.detalle(ID).isEmpty());
    assertThrows(InformacionGalponNoDisponibleException.class, client::todos);
    for (int error : new int[] {400, 401, 403, 500, 503}) {
      status = error;
      assertThrows(InformacionGalponNoDisponibleException.class, () -> client.detalle(ID));
      assertThrows(
          Modulo1NoDisponibleException.class,
          () -> new GalponMedicacionRestQueryAdapter(client).consultar(ID));
    }
  }

  @Test
  void rechazaRespuestasInvalidasSinFabricarDatos() {
    for (String invalido :
        new String[] {
          "no-json",
          "null",
          "{}",
          "{\"id\":\"" + ID + "\",\"nombre\":\"A\",\"estado\":\"PRODUCTIVO\",\"capacidad\":1.5}",
          "{\"id\":\""
              + ID
              + "\",\"nombre\":\"A\",\"estado\":\"PRODUCTIVO\",\"capacidad\":3000000000}"
        }) {
      body = invalido;
      assertThrows(InformacionGalponNoDisponibleException.class, () -> client.detalle(ID));
    }
  }

  @Test
  void conservaEstadosDesconocidosYCambiosAditivos() {
    body =
        body.replace("PRODUCTIVO", "ESTADO_FUTURO")
            .replace("\"capacidad\"", "\"nuevo\":true,\"capacidad\"");
    assertEquals(
        "ESTADO_FUTURO", new GalponRestQueryAdapter(client).estadoVigente(ID).orElseThrow());
  }

  @Test
  void recorrePaginasDeDiezYAplicaOrdenYPaginacionDelModulo2() throws Exception {
    server.removeContext("/api/v1/galpones");
    server.createContext(
        "/api/v1/galpones",
        exchange -> {
          String query = exchange.getRequestURI().getQuery();
          requests.add(query);
          int page = Integer.parseInt(query.split("&")[0].split("=")[1]);
          var elementos = new ArrayList<String>();
          for (int i = (page - 1) * 10; i < Math.min(page * 10, 12); i++) {
            elementos.add(
                "{\"id\":\"00000000-0000-0000-0000-%012d\",\"nombre\":\"G%02d\",\"capacidad\":100,\"estado\":\"PRODUCTIVO\"}"
                    .formatted(i, i));
          }
          byte[] bytes =
              ("{\"contenido\":["
                      + String.join(",", elementos)
                      + "],\"pagina\":"
                      + page
                      + ",\"tamanio\":10,\"totalElementos\":12,\"totalPaginas\":2}")
                  .getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });
    var pagina = new GalponRestQueryAdapter(client).listar(1, 5, "id", false);
    assertEquals(12, pagina.totalElementos());
    assertEquals(5, pagina.elementos().size());
    assertEquals("G06", pagina.elementos().getFirst().nombre());
    assertEquals(2, requests.size());
    assertTrue(requests.getFirst().contains("page=1"));
    assertTrue(requests.getLast().contains("page=2"));
  }

  @Test
  void listaVaciaYPaginaRemotaInvalida() {
    body = "{\"contenido\":[],\"pagina\":1,\"tamanio\":10,\"totalElementos\":0,\"totalPaginas\":0}";
    assertTrue(client.todos().isEmpty());
    body = body.replace("\"pagina\":1", "\"pagina\":0");
    assertThrows(InformacionGalponNoDisponibleException.class, client::todos);
  }

  @Test
  void conexionFallidaEsIndisponibilidad() {
    server.stop(0);
    assertThrows(InformacionGalponNoDisponibleException.class, () -> client.detalle(ID));
  }

  @Test
  void conservaNombreDelLoteEnListadoYDetalleHastaLaRespuestaRest() throws Exception {
    var mapper = new com.avicontrol.infrastructure.adapter.in.rest.galpon.mapper.GalponRestMapper();
    var clock =
        java.time.Clock.fixed(
            java.time.Instant.parse("2026-09-20T12:00:00Z"), java.time.ZoneId.of("America/Bogota"));
    var galpones = new GalponRestQueryAdapter(client);
    var lotes = new LoteRestQueryAdapter(client);
    var detalle =
        mapper.response(
            new com.avicontrol.application.galpon.ConsultarGalponUseCase(galpones, lotes, clock)
                .consultar(ID));
    assertEquals("Lote A1", detalle.loteActual().nombre());
    String detalleJson = body;
    server.createContext(
        "/api/v1/galpones/" + ID,
        exchange -> {
          requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI());
          var bytes = detalleJson.getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });
    var json = tools.jackson.databind.json.JsonMapper.builder().build();
    var resumen = json.readTree(detalleJson);
    ((tools.jackson.databind.node.ObjectNode) resumen).remove("loteActivo");
    body =
        "{\"contenido\":["
            + resumen.toString()
            + "],\"pagina\":1,\"tamanio\":10,\"totalElementos\":1,\"totalPaginas\":1}";
    var listado =
        mapper.response(
            new com.avicontrol.application.galpon.ConsultarListadoGalponesUseCase(galpones, lotes)
                .consultar(0, 20, "nombre,asc"));
    assertEquals("Lote A1", listado.elementos().getFirst().loteActual().nombre());
    assertEquals(detalle.loteActual().id(), listado.elementos().getFirst().loteActual().id());
    assertEquals(
        4, requests.size()); // Detalle y listado con consulta del lote omitido en el resumen.
  }

  private String fixture(String name) throws Exception {
    try (var input = getClass().getResourceAsStream("/contratos/modulo1/" + name)) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
