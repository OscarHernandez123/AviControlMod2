package com.avicontrol.infrastructure.adapter.in.rest.sacrificio.productivo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avicontrol.AviControlModulo2Application;
import com.avicontrol.application.sacrificio.productivo.EjecutarOrdenSacrificioUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
class SacrificioProductivoControllerTest {
  @DynamicPropertySource
  static void baseAislada(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.datasource.url",
        () ->
            "jdbc:h2:mem:ordenes_sacrificio_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
  }

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired Clock clock;
  @Autowired EjecutarOrdenSacrificioUseCase ejecucion;
  private final ObjectMapper json = new ObjectMapper();

  @BeforeEach
  void limpiar() {
    jdbc.update("DELETE FROM ordenes_sacrificio_productivo");
    jdbc.update("DELETE FROM modulo1_galpon_local");
  }

  @Test
  void programaReprogramaCancelaYResume() throws Exception {
    UUID galpon = galpon("EN_COSECHA", UUID.randomUUID());
    UUID orden = programar(galpon);
    mvc.perform(get("/api/ordenes-sacrificio").with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(orden.toString()))
        .andExpect(jsonPath("$[0].estado").value("PROGRAMADA"));
    mvc.perform(get("/api/ordenes-sacrificio")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/ordenes-sacrificio/resumen").with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalOrdenes").value(1))
        .andExpect(jsonPath("$.ordenesPendientes").value(1));
    mvc.perform(
            put("/api/ordenes-sacrificio/{id}/reprogramacion", orden)
                .contentType(MediaType.APPLICATION_JSON)
                .content(fecha(OffsetDateTime.now(clock).plusDays(2)))
                .with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("REPROGRAMADA"));
    mvc.perform(post("/api/ordenes-sacrificio/{id}/cancelacion", orden).with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("CANCELADA"));
    mvc.perform(get("/api/ordenes-sacrificio").with(admin()))
        .andExpect(jsonPath("$[0].estado").value("CANCELADA"));
    mvc.perform(get("/api/ordenes-sacrificio/resumen").with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ordenesPendientes").value(0));
    mvc.perform(post("/api/ordenes-sacrificio/{id}/cancelacion", orden).with(admin()))
        .andExpect(status().isConflict());
  }

  @Test
  void ejecutaUnaVezYFinalizaAlojamiento() throws Exception {
    UUID galpon = galpon("EN_COSECHA", UUID.randomUUID());
    UUID orden = programar(galpon);
    vencer(orden);
    assertTrue(ejecucion.ejecutar(orden));
    assertFalse(ejecucion.ejecutar(orden));
    assertEquals(
        "EJECUTADA",
        jdbc.queryForObject(
            "SELECT estado FROM ordenes_sacrificio_productivo WHERE id=?", String.class, orden));
    assertEquals(
        "VACIADO_SANITARIO",
        jdbc.queryForObject(
            "SELECT estado FROM modulo1_galpon_local WHERE id=?", String.class, galpon));
    assertEquals(
        0,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM modulo1_galpon_local WHERE id=? AND lote_id IS NOT NULL",
            Integer.class,
            galpon));
  }

  @Test
  void noReprogramaNiCancelaDespuesDeLaFechaAunqueSigaPendiente() throws Exception {
    UUID galpon = galpon("EN_COSECHA", UUID.randomUUID());
    UUID orden = programar(galpon);
    vencer(orden);
    mvc.perform(
            put("/api/ordenes-sacrificio/{id}/reprogramacion", orden)
                .contentType(MediaType.APPLICATION_JSON)
                .content(fecha(OffsetDateTime.now(clock).plusDays(2)))
                .with(admin()))
        .andExpect(status().isConflict());
    mvc.perform(post("/api/ordenes-sacrificio/{id}/cancelacion", orden).with(admin()))
        .andExpect(status().isConflict());
  }

  @Test
  void soloUnaEjecucionGanaConSolicitudesSimultaneas() throws Exception {
    UUID galpon = galpon("EN_COSECHA", UUID.randomUUID());
    UUID orden = programar(galpon);
    vencer(orden);
    var inicio = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      var primera =
          pool.submit(
              () -> {
                inicio.await();
                return ejecucion.ejecutar(orden);
              });
      var segunda =
          pool.submit(
              () -> {
                inicio.await();
                return ejecucion.ejecutar(orden);
              });
      inicio.countDown();
      int ganadores =
          (primera.get(10, TimeUnit.SECONDS) ? 1 : 0) + (segunda.get(10, TimeUnit.SECONDS) ? 1 : 0);
      assertEquals(1, ganadores);
    }
  }

  @Test
  void validaFechaEstadoYAutorizacion() throws Exception {
    UUID noApto = galpon("PRODUCTIVA", UUID.randomUUID());
    mvc.perform(
            post("/api/galpones/{id}/ordenes-sacrificio", noApto)
                .contentType(MediaType.APPLICATION_JSON)
                .content(fecha(OffsetDateTime.now(clock).plusDays(1)))
                .with(admin()))
        .andExpect(status().isConflict());
    UUID apto = galpon("EN_COSECHA", UUID.randomUUID());
    mvc.perform(
            post("/api/galpones/{id}/ordenes-sacrificio", apto)
                .contentType(MediaType.APPLICATION_JSON)
                .content(fecha(OffsetDateTime.now(clock).minusDays(1)))
                .with(admin()))
        .andExpect(status().isBadRequest());
    mvc.perform(post("/api/galpones/{id}/ordenes-sacrificio", apto))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/galpones/{id}/ordenes-sacrificio", apto)
                .contentType(MediaType.APPLICATION_JSON)
                .content(fecha(OffsetDateTime.now(clock).plusDays(1)))
                .with(SecurityMockMvcRequestPostProcessors.user("worker").roles("TRABAJADOR")))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/ordenes-sacrificio/resumen").with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalOrdenes").value(0));
  }

  private UUID galpon(String estado, UUID lote) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO modulo1_galpon_local (id,lote_id,estado,poblacion_viva,fecha_ingreso,nombre,aforo_maximo,lote_codigo) VALUES (?,?,?,?,?,?,?,?)",
        id,
        lote,
        estado,
        8000,
        LocalDate.now(clock).minusDays(50),
        "G1",
        9000,
        "L1");
    return id;
  }

  private UUID programar(UUID galpon) throws Exception {
    String body =
        mvc.perform(
                post("/api/galpones/{id}/ordenes-sacrificio", galpon)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(fecha(OffsetDateTime.now(clock).plusDays(1)))
                    .with(admin()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.estado").value("PROGRAMADA"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return UUID.fromString(json.readTree(body).get("id").asText());
  }

  private void vencer(UUID orden) {
    jdbc.update(
        "UPDATE ordenes_sacrificio_productivo SET fecha_hora_programada=? WHERE id=?",
        java.sql.Timestamp.from(Instant.now(clock).minusSeconds(1)),
        orden);
  }

  private String fecha(OffsetDateTime valor) {
    return "{\"fechaHoraProgramada\":\"" + valor + "\"}";
  }

  private SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor admin() {
    return SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR");
  }
}
