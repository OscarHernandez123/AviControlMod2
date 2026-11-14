package com.avicontrol.infrastructure.adapter.in.rest.sanitario.enfermedad;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avicontrol.AviControlModulo2Application;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = AviControlModulo2Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EnfermedadControllerTest {
  @Autowired MockMvc mvc;
  private final ObjectMapper json = new ObjectMapper();

  private static final String BODY =
      """
      {"nombre":"Newcastle","nivelRiesgo":"CRITICO","tipo":"VIRAL",
       "descripcionClinica":"Signos respiratorios","requiereSacrificioSanitario":true}
      """;

  @Test
  void registraYEditaConRolVeterinario() throws Exception {
    String response =
        mvc.perform(
                post("/api/enfermedades")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(BODY)
                    .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.requiereSacrificioSanitario").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode node = json.readTree(response);
    UUID id = UUID.fromString(node.get("id").asText());
    mvc.perform(
            put("/api/enfermedades/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY.replace("true", "false"))
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requiereSacrificioSanitario").value(false));
  }

  @Test
  void rechazaBooleanoAusenteYRolDistinto() throws Exception {
    mvc.perform(
            post("/api/enfermedades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY.replace(",\"requiereSacrificioSanitario\":true", ""))
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/enfermedades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY)
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
        .andExpect(status().isForbidden());
    mvc.perform(
            put("/api/enfermedades/{id}", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY)
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().isNotFound());
    mvc.perform(
            put("/api/enfermedades/{id}", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY)
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
        .andExpect(status().isForbidden());
    mvc.perform(post("/api/enfermedades").contentType(MediaType.APPLICATION_JSON).content(BODY))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void identificaCamposObligatoriosYRechazaEnumInvalido() throws Exception {
    for (String campo :
        new String[] {
          "nombre", "nivelRiesgo", "tipo", "descripcionClinica", "requiereSacrificioSanitario"
        }) {
      ObjectNode body = (ObjectNode) json.readTree(BODY);
      body.remove(campo);
      mvc.perform(
              post("/api/enfermedades")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(json.writeValueAsString(body))
                  .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
          .andExpect(status().isBadRequest())
          .andExpect(
              jsonPath("$.fieldErrors[0]").value(org.hamcrest.Matchers.containsString(campo)));
    }
    mvc.perform(
            post("/api/enfermedades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY.replace("Newcastle", "   "))
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.fieldErrors[0]").value(org.hamcrest.Matchers.containsString("nombre")));
    mvc.perform(
            post("/api/enfermedades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY.replace("VIRAL", "DESCONOCIDA"))
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().isBadRequest());
  }
}
