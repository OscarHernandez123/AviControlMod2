package com.avicontrol.infrastructure.adapter.in.rest.sanitario.diagnostico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avicontrol.AviControlModulo2Application;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = AviControlModulo2Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DiagnosticoControllerTest {
  private static final UUID GALPON = UUID.fromString("550e8400-e29b-41d4-a716-446655440101");
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired Clock clock;
  private final ObjectMapper json = new ObjectMapper();

  @BeforeEach
  void preparar() {
    jdbc.update("DELETE FROM diagnosticos_galpon WHERE galpon_id = ?", GALPON);
    jdbc.update("UPDATE modulo1_galpon_local SET estado = 'AISLAMIENTO' WHERE id = ?", GALPON);
  }

  @Test
  void registraTratamientoYConservaInstantaneaTrasEditarCatalogos() throws Exception {
    UUID enfermedad = crearEnfermedad(false);
    UUID medicacion = crearMedicacion(enfermedad, 5);
    String response =
        mvc.perform(
                post("/api/galpones/{id}/diagnosticos", GALPON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body(enfermedad, medicacion))
                    .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.duracionDias").value(5))
            .andExpect(
                jsonPath("$.fechaReintegro").value(LocalDate.now(clock).plusDays(5).toString()))
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID diagnostico = UUID.fromString(json.readTree(response).get("id").asText());
    mvc.perform(
            get("/api/diagnosticos")
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$[?(@.id == '" + diagnostico + "')].estado")
                .value(org.hamcrest.Matchers.hasItem("ACTIVO")));
    mvc.perform(
            get("/api/diagnosticos")
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/galpones/{id}/diagnosticos", GALPON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(enfermedad, medicacion))
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().isConflict());
    jdbc.update(
        "UPDATE medicaciones SET duracion_dias = 7, dosis = '1 ml', descripcion = 'Nueva pauta' WHERE id = ?",
        medicacion);
    mvc.perform(
            put("/api/enfermedades/{id}", enfermedad)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"nombre":"Otro nombre","nivelRiesgo":"CRITICO","tipo":"VIRAL",
                     "descripcionClinica":"Signos actualizados","requiereSacrificioSanitario":true}
                    """)
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requiereSacrificioSanitario").value(true));
    mvc.perform(
            get("/api/diagnosticos/{id}", diagnostico)
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.duracionDias").value(5))
        .andExpect(jsonPath("$.dosis").value("0.5 ml"))
        .andExpect(jsonPath("$.descripcionMedicacion").value("Aplicar"))
        .andExpect(jsonPath("$.requiereSacrificioSanitario").value(false))
        .andExpect(jsonPath("$.enfermedadNombre").value("Newcastle"));
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM diagnosticos_galpon WHERE id = ?", Integer.class, diagnostico));
  }

  @Test
  void registraEnfermedadSanitariaSinMedicacion() throws Exception {
    UUID enfermedad = crearEnfermedad(true);
    mvc.perform(
            post("/api/galpones/{id}/diagnosticos", GALPON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(enfermedad, null))
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.requiereSacrificioSanitario").value(true))
        .andExpect(jsonPath("$.medicacionId").value((Object) null))
        .andExpect(jsonPath("$.fechaReintegro").value((Object) null));
  }

  @Test
  void rechazaEstadoRolYMedicacionIncompatible() throws Exception {
    UUID enfermedad = crearEnfermedad(false);
    UUID otra = crearEnfermedad(false);
    UUID medicacion = crearMedicacion(otra, 5);
    mvc.perform(
            post("/api/galpones/{id}/diagnosticos", GALPON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(enfermedad, null))
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/galpones/{id}/diagnosticos", GALPON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(enfermedad, medicacion))
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().isConflict());
    mvc.perform(
            post("/api/galpones/{id}/diagnosticos", GALPON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(enfermedad, medicacion))
                .with(SecurityMockMvcRequestPostProcessors.user("trabajador").roles("TRABAJADOR")))
        .andExpect(status().isForbidden());
    jdbc.update("UPDATE modulo1_galpon_local SET estado = 'PRODUCTIVA' WHERE id = ?", GALPON);
    mvc.perform(
            post("/api/galpones/{id}/diagnosticos", GALPON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(enfermedad, medicacion))
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().isConflict());
    assertEquals(
        0,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM diagnosticos_galpon WHERE galpon_id = ?", Integer.class, GALPON));
  }

  private UUID crearEnfermedad(boolean sacrificio) throws Exception {
    String response =
        mvc.perform(
                post("/api/enfermedades")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
            {"nombre":"Newcastle","nivelRiesgo":"ALTO","tipo":"VIRAL",
             "descripcionClinica":"Signos respiratorios","requiereSacrificioSanitario":%s}
            """
                            .formatted(sacrificio))
                    .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return UUID.fromString(json.readTree(response).get("id").asText());
  }

  private UUID crearMedicacion(UUID enfermedad, int dias) {
    UUID id = UUID.randomUUID();
    java.time.Instant now = java.time.Instant.now(clock);
    jdbc.update(
        "INSERT INTO medicaciones (id, enfermedad_id, medicamento_id, dosis, duracion_dias, descripcion, version, fecha_creacion, fecha_actualizacion) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
        id,
        enfermedad,
        UUID.randomUUID(),
        "0.5 ml",
        dias,
        "Aplicar",
        0,
        now,
        now);
    return id;
  }

  private String body(UUID enfermedad, UUID medicacion) {
    return """
        {"enfermedadId":"%s","medicacionId":%s,"observaciones":"Tratamiento prescrito"}
        """
        .formatted(enfermedad, medicacion == null ? "null" : "\"" + medicacion + "\"");
  }
}
