package com.avicontrol.infrastructure.adapter.in.rest.nutricion;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avicontrol.AviControlModulo2Application;
import com.avicontrol.application.nutricion.PlanNutricionalLoteUseCase;
import com.avicontrol.domain.model.catalogo.EtapaCrianza;
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
class PlanNutricionalGalponControllerTest {
  @Autowired MockMvc mvc;
  @MockitoBean PlanNutricionalLoteUseCase useCase;

  @Test
  void consultaCompatiblesYSegregaRolesDeModificacion() throws Exception {
    when(useCase.compatibles(EtapaCrianza.INICIO)).thenReturn(List.of());

    mvc.perform(
            get("/api/nutricion/alimentos-compatibles")
                .param("tipoAlimento", "INICIO")
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());

    mvc.perform(
            put("/api/galpones/{galponId}/plan-nutricional/sustituir", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"plantillaId\":\"" + UUID.randomUUID() + "\"}")
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"));

    UUID galponId = UUID.randomUUID();
    when(useCase.consultar(galponId)).thenThrow(new NoSuchElementException("Sin plan"));
    mvc.perform(
            get("/api/galpones/{galponId}/plan-nutricional", galponId)
                .with(SecurityMockMvcRequestPostProcessors.user("nutri").roles("NUTRICIONISTA")))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
  }
}
