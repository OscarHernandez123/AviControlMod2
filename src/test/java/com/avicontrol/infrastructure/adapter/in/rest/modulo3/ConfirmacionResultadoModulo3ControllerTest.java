package com.avicontrol.infrastructure.adapter.in.rest.modulo3;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.avicontrol.AviControlModulo2Application;
import java.util.List;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(
    classes = AviControlModulo2Application.class,
    properties = {
      "spring.datasource.url=jdbc:h2:mem:confirmacion_m3_rest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
      "avicontrol.security.modulo3.enabled=true",
      "avicontrol.security.modulo3.username=m3_rest_test",
      "avicontrol.security.modulo3.password=clave-solo-pruebas-rest",
      "avicontrol.modulo3.resultado-utilizado.enabled=false"
    })
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConfirmacionResultadoModulo3ControllerTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  private final UUID lote = UUID.randomUUID();
  private final UUID resultado = UUID.randomUUID();
  private UUID orden;
  private final UUID evento = UUID.randomUUID();

  @BeforeEach
  void crearResultado() {
    orden = crearResultado(resultado, lote);
  }

  @Test
  void confirmaReintentaYBloqueaCorreccionSinKafka() throws Exception {
    String aviso = aviso(evento, resultado, lote);
    for (int i = 0; i < 2; i++) {
      mvc.perform(enviar(aviso))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.eventId").value(evento.toString()))
          .andExpect(jsonPath("$.resultadoId").value(resultado.toString()))
          .andExpect(jsonPath("$.loteId").value(lote.toString()))
          .andExpect(jsonPath("$.estado").value("UTILIZADO"));
    }
    assertEquals(1, confirmaciones(evento));
    assertEquals(
        1L,
        jdbc.queryForObject(
            "SELECT version FROM resultados_sacrificio_productivo WHERE id=?",
            Long.class,
            resultado));
    mvc.perform(
            get("/api/integracion/modulo3/lotes/{id}/resultado-sacrificio", lote)
                .with(httpBasic("m3_rest_test", "clave-solo-pruebas-rest")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("UTILIZADO"));
    mvc.perform(
            put("/api/ordenes-sacrificio/{id}/resultados", orden)
                .with(user("admin").roles("ADMINISTRADOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidadFinalPollos\":99,\"pesoTotalKg\":249.00}"))
        .andExpect(status().isConflict());
    assertEquals(
        100,
        jdbc.queryForObject(
            "SELECT cantidad_final_pollos FROM resultados_sacrificio_productivo WHERE id=?",
            Integer.class,
            resultado));
  }

  @Test
  void toleraCamposAdicionalesDelSobreYPayload() throws Exception {
    String aviso =
        aviso(evento, resultado, lote)
            .replace("\"eventType\"", "\"datoFuturo\":true,\"eventType\"")
            .replace("\"loteId\"", "\"datoFuturo\":\"extra\",\"loteId\"");
    mvc.perform(enviar(aviso)).andExpect(status().isOk());
    assertEquals(1, confirmaciones(evento));
  }

  @Test
  void exigeIdentidadM3YDeniegaOtrosRoles() throws Exception {
    String aviso = aviso(evento, resultado, lote);
    mvc.perform(
            post(ConfirmacionResultadoModulo3Controller.RUTA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(aviso))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post(ConfirmacionResultadoModulo3Controller.RUTA)
                .with(httpBasic("m3_rest_test", "incorrecta"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(aviso))
        .andExpect(status().isUnauthorized());
    for (String rol : List.of("ADMINISTRADOR", "VETERINARIO", "TRABAJADOR", "NUTRICIONISTA")) {
      mvc.perform(
              post(ConfirmacionResultadoModulo3Controller.RUTA)
                  .with(user("otro").roles(rol))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(aviso))
          .andExpect(status().isForbidden());
    }
    assertEquals(0, confirmaciones(evento));
    assertDisponible(resultado);
  }

  @Test
  void rechazaSobresIncompletosInvalidosYVersionesNoSoportadas() throws Exception {
    String valido = aviso(evento, resultado, lote);
    for (String invalido :
        List.of(
            "{",
            "null",
            "{}",
            valido.replace("\"eventId\":\"" + evento + "\"", "\"eventId\":null"),
            valido.replace(evento.toString(), "uuid-invalido"),
            valido.replace("ResultadoSacrificioUtilizado", "OtroEvento"),
            valido.replace("\"eventVersion\":1", "\"eventVersion\":2"),
            valido.replace("\"eventVersion\":1", "\"eventVersion\":1.5"),
            valido.replace("\"eventVersion\":1", "\"eventVersion\":1.0"),
            valido.replace("\"eventVersion\":1", "\"eventVersion\":\"1\""),
            valido.replace("\"eventVersion\":1", "\"eventVersion\":4294967297"),
            valido.replace("\"eventVersion\":1", "\"eventVersion\":null"),
            valido.replace("\"resultadoId\":\"" + resultado + "\"", "\"resultadoId\":null"),
            valido.replace("\"loteId\":\"" + lote + "\"", "\"loteId\":null"),
            valido.replace(
                "\"aggregateId\":\"" + resultado + "\"",
                "\"aggregateId\":\"" + UUID.randomUUID() + "\""))) {
      mvc.perform(enviar(invalido)).andExpect(status().isBadRequest());
    }
    assertEquals(0, confirmaciones(evento));
    assertDisponible(resultado);
  }

  @Test
  void resultadoInexistenteDevuelve404SinConsumirEvento() throws Exception {
    mvc.perform(enviar(aviso(evento, UUID.randomUUID(), lote))).andExpect(status().isNotFound());
    assertEquals(0, confirmaciones(evento));
    mvc.perform(enviar(aviso(evento, resultado, lote))).andExpect(status().isOk());
  }

  @Test
  void loteIncorrectoDevuelve409YPermiteReintentarConDatosCorrectos() throws Exception {
    mvc.perform(enviar(aviso(evento, resultado, UUID.randomUUID())))
        .andExpect(status().isConflict());
    assertEquals(0, confirmaciones(evento));
    assertDisponible(resultado);
    mvc.perform(enviar(aviso(evento, resultado, lote))).andExpect(status().isOk());
  }

  @Test
  void reutilizarEventoParaOtroResultadoNoBloqueaElSegundo() throws Exception {
    UUID otroResultado = UUID.randomUUID();
    UUID otroLote = UUID.randomUUID();
    crearResultado(otroResultado, otroLote);
    mvc.perform(enviar(aviso(evento, resultado, lote))).andExpect(status().isOk());
    mvc.perform(enviar(aviso(evento, otroResultado, otroLote))).andExpect(status().isConflict());
    assertEquals(1, confirmaciones(evento));
    assertDisponible(otroResultado);
  }

  @Test
  void dosConfirmacionesSimultaneasDelMismoEventoTienenUnSoloEfecto() throws Exception {
    var inicio = new CountDownLatch(1);
    String aviso = aviso(evento, resultado, lote);
    try (var pool = Executors.newFixedThreadPool(2)) {
      var uno =
          pool.submit(
              () -> {
                inicio.await();
                return mvc.perform(enviar(aviso)).andReturn().getResponse().getStatus();
              });
      var dos =
          pool.submit(
              () -> {
                inicio.await();
                return mvc.perform(enviar(aviso)).andReturn().getResponse().getStatus();
              });
      inicio.countDown();
      assertEquals(200, uno.get(15, TimeUnit.SECONDS));
      assertEquals(200, dos.get(15, TimeUnit.SECONDS));
    }
    assertEquals(1, confirmaciones(evento));
    assertEquals(
        1L,
        jdbc.queryForObject(
            "SELECT version FROM resultados_sacrificio_productivo WHERE id=?",
            Long.class,
            resultado));
  }

  private MockHttpServletRequestBuilder enviar(String aviso) {
    return post(ConfirmacionResultadoModulo3Controller.RUTA)
        .with(httpBasic("m3_rest_test", "clave-solo-pruebas-rest"))
        .contentType(MediaType.APPLICATION_JSON)
        .content(aviso);
  }

  private String aviso(UUID eventId, UUID resultadoId, UUID loteId) {
    return """
        {"eventId":"%s","eventType":"ResultadoSacrificioUtilizado","eventVersion":1,
        "aggregateId":"%s","payload":{"resultadoId":"%s","loteId":"%s"}}
        """
        .formatted(eventId, resultadoId, resultadoId, loteId);
  }

  private int confirmaciones(UUID eventId) {
    return jdbc.queryForObject(
        "SELECT COUNT(*) FROM confirmaciones_uso_resultado_sacrificio WHERE evento_id=?",
        Integer.class,
        eventId);
  }

  private void assertDisponible(UUID resultadoId) {
    assertEquals(
        "DISPONIBLE",
        jdbc.queryForObject(
            "SELECT estado FROM resultados_sacrificio_productivo WHERE id=?",
            String.class,
            resultadoId));
  }

  private UUID crearResultado(UUID resultadoId, UUID loteId) {
    UUID ordenId = UUID.randomUUID();
    UUID galponId = UUID.randomUUID();
    jdbc.update(
        """
        INSERT INTO ordenes_sacrificio_productivo
        (id,galpon_id,lote_id,estado,fecha_hora_programada,fecha_creacion,lote_reservado_id)
        VALUES (?,?,?,'EJECUTADA',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,?)
        """,
        ordenId,
        galponId,
        loteId,
        loteId);
    jdbc.update(
        """
        INSERT INTO resultados_sacrificio_productivo
        (id,orden_id,galpon_id,lote_id,cantidad_final_pollos,peso_total_kg,estado,fecha_registro)
        VALUES (?,?,?,?,100,250.50,'DISPONIBLE',CURRENT_TIMESTAMP)
        """,
        resultadoId,
        ordenId,
        galponId,
        loteId);
    return ordenId;
  }
}
