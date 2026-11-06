package com.avicontrol.infrastructure.adapter.in.rest.sanitario.aislamiento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avicontrol.AviControlModulo2Application;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = AviControlModulo2Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SolicitudAislamientoControllerTest {
  private static final UUID GALPON = UUID.fromString("550e8400-e29b-41d4-a716-446655440101");
  private static final String BODY =
      """
      {"motivo":"Signos respiratorios","signosClinicos":"Tos y decaimiento"}
      """;
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  private final ObjectMapper json = new ObjectMapper();

  @BeforeEach
  void limpiar() {
    jdbc.update("DELETE FROM solicitudes_aislamiento WHERE galpon_id = ?", GALPON);
    jdbc.update(
        "UPDATE modulo1_galpon_local SET estado = 'PRODUCTIVA', poblacion_viva = 8000 WHERE id = ?",
        GALPON);
  }

  @Test
  void creaPendienteVisibleAlVeterinarioSinAislar() throws Exception {
    String response =
        mvc.perform(
                post("/api/galpones/{id}/solicitudes-aislamiento", GALPON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(BODY)
                    .with(
                        SecurityMockMvcRequestPostProcessors.user("trabajador")
                            .roles("TRABAJADOR")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.estado").value("PENDIENTE_VALIDACION"))
            .andExpect(jsonPath("$.poblacionVivaReportada").value(8000))
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID solicitud = UUID.fromString(json.readTree(response).get("id").asText());
    assertEquals(
        "PRODUCTIVA",
        jdbc.queryForObject(
            "SELECT estado FROM modulo1_galpon_local WHERE id = ?", String.class, GALPON));
    mvc.perform(
            get("/api/solicitudes-aislamiento/pendientes")
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(solicitud.toString()));
    mvc.perform(
            get("/api/solicitudes-aislamiento/{id}", solicitud)
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.motivo").value("Signos respiratorios"));
  }

  @Test
  void bloqueaDuplicadoEstadoInvalidoYRol() throws Exception {
    var trabajador = SecurityMockMvcRequestPostProcessors.user("trabajador").roles("TRABAJADOR");
    mvc.perform(
            post("/api/galpones/{id}/solicitudes-aislamiento", GALPON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY)
                .with(trabajador))
        .andExpect(status().isCreated());
    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            jdbc.update(
                "INSERT INTO solicitudes_aislamiento (id, galpon_id, pending_galpon_id, lote_id, motivo, signos_clinicos, poblacion_viva_reportada, edad_dias_reportada, trabajador, fecha_emision, estado, version) SELECT ?, galpon_id, pending_galpon_id, lote_id, motivo, signos_clinicos, poblacion_viva_reportada, edad_dias_reportada, trabajador, fecha_emision, estado, version FROM solicitudes_aislamiento WHERE galpon_id = ?",
                UUID.randomUUID(),
                GALPON));
    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            jdbc.update(
                "INSERT INTO solicitudes_aislamiento (id, galpon_id, pending_galpon_id, lote_id, motivo, signos_clinicos, poblacion_viva_reportada, edad_dias_reportada, trabajador, fecha_emision, estado, version) SELECT ?, galpon_id, NULL, lote_id, motivo, signos_clinicos, poblacion_viva_reportada, edad_dias_reportada, trabajador, fecha_emision, estado, version FROM solicitudes_aislamiento WHERE galpon_id = ?",
                UUID.randomUUID(),
                GALPON));
    mvc.perform(
            post("/api/galpones/{id}/solicitudes-aislamiento", GALPON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY)
                .with(trabajador))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONFLICTO_AISLAMIENTO"));
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM solicitudes_aislamiento WHERE galpon_id = ?",
            Integer.class,
            GALPON));
    mvc.perform(
            post("/api/galpones/{id}/solicitudes-aislamiento", GALPON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY.replace("Tos y decaimiento", " "))
                .with(trabajador))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/galpones/{id}/solicitudes-aislamiento", GALPON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY)
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().isForbidden());
    jdbc.update("UPDATE modulo1_galpon_local SET estado = 'AISLAMIENTO' WHERE id = ?", GALPON);
    mvc.perform(
            post("/api/galpones/{id}/solicitudes-aislamiento", GALPON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY)
                .with(trabajador))
        .andExpect(status().isConflict());
    mvc.perform(
            post("/api/galpones/{id}/solicitudes-aislamiento", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY)
                .with(trabajador))
        .andExpect(status().isNotFound());
    jdbc.update(
        "UPDATE modulo1_galpon_local SET estado = 'PRODUCTIVA', poblacion_viva = 0 WHERE id = ?",
        GALPON);
    mvc.perform(
            post("/api/galpones/{id}/solicitudes-aislamiento", GALPON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY)
                .with(trabajador))
        .andExpect(status().isConflict());
  }
}
