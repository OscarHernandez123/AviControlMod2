package com.avicontrol.infrastructure.adapter.in.rest.medicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avicontrol.AviControlModulo2Application;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
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
class MedicacionControllerTest {
  private static final UUID MEDICAMENTO = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
  @Autowired MockMvc mvc;
  private final ObjectMapper json = new ObjectMapper();
  @Autowired JdbcTemplate jdbc;

  @Test
  void registraYEditaSinMoverInventario() throws Exception {
    UUID enfermedad = crearEnfermedad();
    String lote = "MED-PLAN-008-" + UUID.randomUUID();
    mvc.perform(
            post("/api/recepciones/medicamentos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
            {"medicamentoId":"%s","codigoLote":"%s","presentacion":"Frasco",
             "fechaIngreso":"2026-11-05","fechaVencimiento":"2027-05-05",
             "cantidadPresentaciones":2,"contenidoPorPresentacion":500,
             "unidadContenido":"MILILITRO","precioNetoPorPresentacion":1000,"impuestoPorcentaje":0}
            """
                        .formatted(MEDICAMENTO, lote))
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
        .andExpect(status().isCreated());
    Integer movements =
        jdbc.queryForObject("SELECT COUNT(*) FROM movimientos_inventario", Integer.class);
    String body = body(enfermedad, MEDICAMENTO, 5);
    String response =
        mvc.perform(
                post("/api/medicaciones")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
                    .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.duracionDias").value(5))
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID medicacion = UUID.fromString(json.readTree(response).get("id").asText());
    mvc.perform(
            put("/api/medicaciones/{id}", medicacion)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(enfermedad, MEDICAMENTO, 7))
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.duracionDias").value(7));
    assertEquals(
        movements,
        jdbc.queryForObject("SELECT COUNT(*) FROM movimientos_inventario", Integer.class));
  }

  @Test
  void rechazaReferenciasDuracionYRol() throws Exception {
    UUID enfermedad = crearEnfermedad();
    mvc.perform(
            post("/api/medicaciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(UUID.randomUUID(), MEDICAMENTO, 5))
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().isNotFound());
    mvc.perform(
            post("/api/medicaciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(enfermedad, UUID.randomUUID(), 5))
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().isNotFound());
    mvc.perform(
            post("/api/medicaciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(enfermedad, MEDICAMENTO, 0))
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/medicaciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"enfermedadId\":\""
                        + enfermedad
                        + "\",\"medicamentoId\":\""
                        + MEDICAMENTO
                        + "\",\"dosis\":\"1 ml\",\"duracionDias\":5,\"descripcion\":\"Agua\"}")
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/medicaciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(enfermedad, MEDICAMENTO, 5))
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
        .andExpect(status().isForbidden());
    mvc.perform(
            put("/api/medicaciones/{id}", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(enfermedad, MEDICAMENTO, 5))
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().isNotFound());
  }

  private UUID crearEnfermedad() throws Exception {
    String response =
        mvc.perform(
                post("/api/enfermedades")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
            {"nombre":"Newcastle","nivelRiesgo":"ALTO","tipo":"VIRAL",
             "descripcionClinica":"Signos respiratorios","requiereSacrificioSanitario":false}
            """)
                    .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return UUID.fromString(json.readTree(response).get("id").asText());
  }

  private String body(UUID enfermedad, UUID medicamento, int dias) {
    return """
        {"enfermedadId":"%s","medicamentoId":"%s","dosis":"0.5 ml por ave",
         "duracionDias":%d,"descripcion":"Administrar según indicación veterinaria",
         "nombreMedicamento":"Fármaco ejemplo","principioActivo":"Principio ejemplo"}
        """
        .formatted(enfermedad, medicamento, dias);
  }
}
