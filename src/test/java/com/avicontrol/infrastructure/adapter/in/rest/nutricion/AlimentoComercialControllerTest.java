package com.avicontrol.infrastructure.adapter.in.rest.nutricion;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avicontrol.AviControlModulo2Application;
import com.avicontrol.application.nutricion.RegistrarAlimentoComercialUseCase;
import com.avicontrol.domain.model.catalogo.EtapaCrianza;
import com.avicontrol.domain.port.out.nutricion.AlimentoCompatibleQueryPort;
import java.math.BigDecimal;
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
class AlimentoComercialControllerTest {
  @Autowired MockMvc mvc;
  @MockitoBean RegistrarAlimentoComercialUseCase useCase;

  private static final String BODY =
      """
      {"nombre":"Iniciador Pro","tipoAlimento":"INICIO","pesoNominalPorBultoKg":40,
       "proteinaPorcentaje":21.5,"energiaKcalKg":3050,"duracionRecomendadaEtapaDias":14}
      """;

  @Test
  void registraAlimentoSoloConRolNutricionista() throws Exception {
    UUID id = UUID.randomUUID();
    when(useCase.registrar(any()))
        .thenReturn(
            new AlimentoCompatibleQueryPort.Alimento(
                id,
                "Iniciador Pro",
                EtapaCrianza.INICIO,
                new BigDecimal("40"),
                new BigDecimal("21.5"),
                new BigDecimal("3050"),
                14,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "SIN_STOCK"));

    mvc.perform(
            post("/api/nutricion/alimentos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY)
                .with(SecurityMockMvcRequestPostProcessors.user("nutri").roles("NUTRICIONISTA")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.nombre").value("Iniciador Pro"));

    mvc.perform(
            post("/api/nutricion/alimentos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY)
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"));

    mvc.perform(
            post("/api/nutricion/alimentos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
        .andExpect(status().isUnauthorized());
  }
}
