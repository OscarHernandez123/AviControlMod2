package com.avicontrol.infrastructure.adapter.in.rest.galpon;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class GalponEdadControllerTest {
  @DynamicPropertySource
  static void baseAislada(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.datasource.url",
        () ->
            "jdbc:h2:mem:galpon_edad_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
  }

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired Clock clock;

  @BeforeEach
  void limpiar() {
    jdbc.update("DELETE FROM modulo1_galpon_local");
  }

  @Test
  void edadInclusivaYGalponSinLote() throws Exception {
    UUID conLote = crear("PRODUCTIVA", UUID.randomUUID(), LocalDate.now(clock).minusDays(16));
    UUID sinLote = crear("DISPONIBLE", null, null);
    mvc.perform(get("/api/galpones/{id}/edad-lote", conLote).with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.edad.diasTotales").value(17))
        .andExpect(jsonPath("$.edad.semanasCompletas").value(2))
        .andExpect(jsonPath("$.edad.diasRestantes").value(3))
        .andExpect(jsonPath("$.edad.texto").value("2 semanas y 3 días (17 días)"));
    mvc.perform(get("/api/galpones/{id}/edad-lote", sinLote).with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.loteId").isEmpty())
        .andExpect(jsonPath("$.edad").isEmpty())
        .andExpect(jsonPath("$.mensaje").value("El galpón no tiene lote alojado actualmente"));
  }

  @Test
  void mapeaAusenciaInconsistenciaYUuidInvalido() throws Exception {
    UUID futuro = crear("PRODUCTIVA", UUID.randomUUID(), LocalDate.now(clock).plusDays(1));
    mvc.perform(get("/api/galpones/{id}/edad-lote", futuro).with(admin()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONFLICTO_GALPON"));
    mvc.perform(get("/api/galpones/{id}/edad-lote", UUID.randomUUID()).with(admin()))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/galpones/no-es-uuid/edad-lote").with(admin()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"));
  }

  @Test
  void soloAdministradorPuedeConsultar() throws Exception {
    UUID id = UUID.randomUUID();
    mvc.perform(get("/api/galpones/{id}/edad-lote", id)).andExpect(status().isUnauthorized());
    mvc.perform(
            get("/api/galpones/{id}/edad-lote", id)
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
        "Galpón 1",
        9000,
        lote == null ? null : "LOTE-1");
    return id;
  }

  private SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor admin() {
    return SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR");
  }
}
