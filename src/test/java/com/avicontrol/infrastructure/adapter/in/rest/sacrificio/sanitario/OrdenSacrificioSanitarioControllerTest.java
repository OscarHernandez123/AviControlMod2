package com.avicontrol.infrastructure.adapter.in.rest.sacrificio.sanitario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avicontrol.AviControlModulo2Application;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
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
class OrdenSacrificioSanitarioControllerTest {
  @DynamicPropertySource
  static void db(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.datasource.url",
        () ->
            "jdbc:h2:mem:sacrificio_sanitario_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
  }

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  private final ObjectMapper json = new ObjectMapper();

  @BeforeEach
  void limpiar() {
    jdbc.update("DELETE FROM ordenes_sacrificio_sanitario");
    jdbc.update("DELETE FROM diagnosticos_galpon");
    jdbc.update("DELETE FROM enfermedades");
    jdbc.update("DELETE FROM modulo1_galpon_local");
  }

  @Test
  void emiteYConfirmaPoblacionVigenteUnaSolaVez() throws Exception {
    UUID galpon = fixture(true, "AISLAMIENTO", 800);
    String body =
        mvc.perform(
                post("/api/galpones/{id}/ordenes-sacrificio-sanitario", galpon).with(veterinario()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.estado").value("PENDIENTE"))
            .andExpect(jsonPath("$.poblacionSacrificada").doesNotExist())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID orden = UUID.fromString(json.readTree(body).get("id").asText());
    assertEquals(
        800,
        jdbc.queryForObject(
            "SELECT poblacion_viva FROM modulo1_galpon_local WHERE id=?", Integer.class, galpon));
    jdbc.update("UPDATE modulo1_galpon_local SET poblacion_viva=750 WHERE id=?", galpon);
    mvc.perform(post("/api/ordenes-sacrificio-sanitario/{id}/ejecucion", orden).with(veterinario()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("EJECUTADA"))
        .andExpect(jsonPath("$.poblacionSacrificada").value(750));
    assertEquals(
        0,
        jdbc.queryForObject(
            "SELECT poblacion_viva FROM modulo1_galpon_local WHERE id=?", Integer.class, galpon));
    assertEquals(
        "VACIADO_SANITARIO",
        jdbc.queryForObject(
            "SELECT estado FROM modulo1_galpon_local WHERE id=?", String.class, galpon));
    mvc.perform(get("/api/ordenes-sacrificio-sanitario").with(veterinario()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(orden.toString()))
        .andExpect(jsonPath("$[0].estado").value("EJECUTADA"))
        .andExpect(jsonPath("$[0].poblacionSacrificada").value(750));
    mvc.perform(get("/api/ordenes-sacrificio-sanitario"))
        .andExpect(status().isUnauthorized());
    mvc.perform(post("/api/ordenes-sacrificio-sanitario/{id}/ejecucion", orden).with(veterinario()))
        .andExpect(status().isConflict());
  }

  @Test
  void rechazaNoElegiblePoblacionCeroYRoles() throws Exception {
    UUID noElegible = fixture(false, "AISLAMIENTO", 800);
    mvc.perform(
            post("/api/galpones/{id}/ordenes-sacrificio-sanitario", noElegible).with(veterinario()))
        .andExpect(status().isConflict());
    UUID galpon = fixture(true, "AISLAMIENTO", 800);
    mvc.perform(post("/api/galpones/{id}/ordenes-sacrificio-sanitario", galpon))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/galpones/{id}/ordenes-sacrificio-sanitario", galpon)
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
        .andExpect(status().isForbidden());
    mvc.perform(post("/api/galpones/no-uuid/ordenes-sacrificio-sanitario").with(veterinario()))
        .andExpect(status().isBadRequest());
    jdbc.update("UPDATE modulo1_galpon_local SET poblacion_viva=0 WHERE id=?", galpon);
    mvc.perform(post("/api/galpones/{id}/ordenes-sacrificio-sanitario", galpon).with(veterinario()))
        .andExpect(status().isConflict());
  }

  @Test
  void confirmacionFallidaNoCambiaPoblacionNiOrden() throws Exception {
    UUID galpon = fixture(true, "AISLAMIENTO", 800);
    String body =
        mvc.perform(
                post("/api/galpones/{id}/ordenes-sacrificio-sanitario", galpon).with(veterinario()))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID orden = UUID.fromString(json.readTree(body).get("id").asText());
    jdbc.update("UPDATE modulo1_galpon_local SET estado='MANTENIMIENTO' WHERE id=?", galpon);
    mvc.perform(post("/api/ordenes-sacrificio-sanitario/{id}/ejecucion", orden).with(veterinario()))
        .andExpect(status().isConflict());
    assertEquals(
        800,
        jdbc.queryForObject(
            "SELECT poblacion_viva FROM modulo1_galpon_local WHERE id=?", Integer.class, galpon));
    assertEquals(
        "PENDIENTE",
        jdbc.queryForObject(
            "SELECT estado FROM ordenes_sacrificio_sanitario WHERE id=?", String.class, orden));
  }

  private UUID fixture(boolean sanitario, String estado, int poblacion) {
    UUID galpon = UUID.randomUUID(), lote = UUID.randomUUID(), enfermedad = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO modulo1_galpon_local (id,lote_id,estado,poblacion_viva,fecha_ingreso,nombre,aforo_maximo,lote_codigo) VALUES (?,?,?,?,?,?,?,?)",
        galpon,
        lote,
        estado,
        poblacion,
        LocalDate.now().minusDays(40),
        "G1",
        1000,
        "L1");
    jdbc.update(
        "INSERT INTO enfermedades (id,nombre,nivel_riesgo,tipo,descripcion_clinica,requiere_sacrificio_sanitario,fecha_creacion,fecha_actualizacion) VALUES (?,?,?,?,?,?,?,?)",
        enfermedad,
        "E1",
        "CRITICO",
        "VIRAL",
        "Descripción",
        true,
        Timestamp.from(Instant.now()),
        Timestamp.from(Instant.now()));
    jdbc.update(
        "INSERT INTO diagnosticos_galpon (id,galpon_id,active_galpon_id,lote_id,enfermedad_id,enfermedad_nombre,requiere_sacrificio_sanitario,observaciones,fecha_diagnostico,estado) VALUES (?,?,?,?,?,?,?,?,?,?)",
        UUID.randomUUID(),
        galpon,
        galpon,
        lote,
        enfermedad,
        "E1",
        true,
        "Observaciones",
        LocalDate.now(),
        "ACTIVO");
    if (!sanitario)
      jdbc.update(
          "UPDATE enfermedades SET requiere_sacrificio_sanitario=FALSE WHERE id=?", enfermedad);
    return galpon;
  }

  private SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor veterinario() {
    return SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO");
  }
}
