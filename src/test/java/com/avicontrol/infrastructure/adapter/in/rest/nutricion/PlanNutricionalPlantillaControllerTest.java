package com.avicontrol.infrastructure.adapter.in.rest.nutricion;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avicontrol.AviControlModulo2Application;
import com.avicontrol.application.nutricion.GestionarPlantillasUseCase;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = AviControlModulo2Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlanNutricionalPlantillaControllerTest {
  @Autowired MockMvc mvc;
  @MockitoBean GestionarPlantillasUseCase useCase;

  @Test
  void listaYSegregaRolesEnCreacionYConsulta() throws Exception {
    when(useCase.listar()).thenReturn(List.of());

    mvc.perform(
            get("/api/nutricion/plantillas")
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
        .andExpect(status().isOk());

    mvc.perform(
            post("/api/nutricion/plantillas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"nombre\":\"P1\",\"tipoAve\":\"ROSS_308\",\"esPredeterminado\":true,\"etapas\":[]}")
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"));

    UUID id = UUID.randomUUID();
    when(useCase.consultar(id)).thenThrow(new NoSuchElementException("Plantilla no encontrada"));
    mvc.perform(
            get("/api/nutricion/plantillas/{id}", id)
                .with(SecurityMockMvcRequestPostProcessors.user("nutri").roles("NUTRICIONISTA")))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
  }
}
