package com.avicontrol.infrastructure.adapter.in.rest.sanitario;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avicontrol.application.sanitario.enfermedad.EditarEnfermedadUseCase;
import com.avicontrol.application.sanitario.enfermedad.RegistrarEnfermedadUseCase;
import com.avicontrol.application.support.CurrentActor;
import com.avicontrol.domain.model.sanitario.enfermedad.Enfermedad;
import com.avicontrol.domain.port.out.sanitario.EnfermedadRepositoryPort;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class EnfermedadControllerTest {
  private MockMvc mockMvc;
  private Map<UUID, Enfermedad> values;

  @BeforeEach
  void setUp() {
    values = new HashMap<>();
    EnfermedadRepositoryPort repository =
        new EnfermedadRepositoryPort() {
          @Override
          public Enfermedad guardar(Enfermedad enfermedad) {
            values.put(enfermedad.id(), enfermedad);
            return enfermedad;
          }

          @Override
          public Optional<Enfermedad> buscarPorId(UUID id) {
            return Optional.ofNullable(values.get(id));
          }
        };
    var actor =
        (com.avicontrol.application.support.CurrentActorProvider)
            () -> new CurrentActor("vet", java.util.Set.of("ROLE_VETERINARIO"));
    mockMvc =
        MockMvcBuilders.standaloneSetup(
                new EnfermedadController(
                    new RegistrarEnfermedadUseCase(repository, actor),
                    new EditarEnfermedadUseCase(repository, actor)))
            .build();
  }

  @Test
  void registersAndEditsThroughBothHttpEndpoints() throws Exception {
    String body =
        "{\"nombre\":\"Newcastle\",\"nivelRiesgo\":\"ALTO\",\"tipo\":\"VIRAL\",\"descripcionClinica\":\"Respiratoria\",\"requiereSacrificioSanitario\":true}";
    String id =
        mockMvc
            .perform(
                post("/api/enfermedades").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.requiereSacrificioSanitario").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString()
            .replaceAll(".*\\\"id\\\":\\\"([^\\\"]+).*", "$1");
    mockMvc
        .perform(
            put("/api/enfermedades/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.replace("true", "false")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requiereSacrificioSanitario").value(false));
  }
}
