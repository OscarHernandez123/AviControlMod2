package com.avicontrol.infrastructure.adapter.in.rest.sacrificio.productivo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avicontrol.AviControlModulo2Application;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = AviControlModulo2Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AptitudSacrificioControllerTest {
  @DynamicPropertySource
  static void baseAislada(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.datasource.url",
        () ->
            "jdbc:h2:mem:aptitud_sacrificio_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
  }

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired Clock clock;

  @BeforeEach
  void limpiar() {
    jdbc.update("DELETE FROM modulo1_galpon_local");
  }

  @Test
  void aptoCambiaEstadoYNoAptoConservaEstado() throws Exception {
    UUID apto = crear("PRODUCTIVA", UUID.randomUUID(), LocalDate.now(clock).minusDays(44));
    UUID noApto = crear("MANTENIMIENTO", UUID.randomUUID(), LocalDate.now(clock).minusDays(43));
    mvc.perform(post("/api/galpones/{id}/validacion-aptitud-sacrificio", apto).with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.apto").value(true))
        .andExpect(jsonPath("$.estadoGalponResultante").value("EN_COSECHA"));
    mvc.perform(post("/api/galpones/{id}/validacion-aptitud-sacrificio", noApto).with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.apto").value(false))
        .andExpect(jsonPath("$.motivosNoAptitud.length()").value(2));
    org.junit.jupiter.api.Assertions.assertEquals(
        "EN_COSECHA",
        jdbc.queryForObject(
            "SELECT estado FROM modulo1_galpon_local WHERE id=?", String.class, apto));
    org.junit.jupiter.api.Assertions.assertEquals(
        "MANTENIMIENTO",
        jdbc.queryForObject(
            "SELECT estado FROM modulo1_galpon_local WHERE id=?", String.class, noApto));
  }

  @Test
  void ausenciaFechaInvalidaYSeguridad() throws Exception {
    UUID sinLote = crear("PRODUCTIVA", null, null);
    mvc.perform(post("/api/galpones/{id}/validacion-aptitud-sacrificio", sinLote).with(admin()))
        .andExpect(status().isNotFound());
    UUID futuro = crear("PRODUCTIVA", UUID.randomUUID(), LocalDate.now(clock).plusDays(1));
    mvc.perform(post("/api/galpones/{id}/validacion-aptitud-sacrificio", futuro).with(admin()))
        .andExpect(status().isConflict());
    mvc.perform(post("/api/galpones/no-uuid/validacion-aptitud-sacrificio").with(admin()))
        .andExpect(status().isBadRequest());
    mvc.perform(post("/api/galpones/{id}/validacion-aptitud-sacrificio", futuro))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/galpones/{id}/validacion-aptitud-sacrificio", futuro)
                .with(SecurityMockMvcRequestPostProcessors.user("worker").roles("TRABAJADOR")))
        .andExpect(status().isForbidden());
  }

  private UUID crear(String estado, UUID lote, LocalDate ingreso) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO modulo1_galpon_local (id,lote_id,estado,poblacion_viva,fecha_ingreso,nombre,aforo_maximo,lote_codigo) VALUES (?,?,?,?,?,?,?,?)",
        id,
        lote,
        estado,
        lote == null ? 0 : 8000,
        ingreso,
        "G1",
        9000,
        lote == null ? null : "L1");
    return id;
  }

  private SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor admin() {
    return SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR");
  }
}
