package com.avicontrol.infrastructure.adapter.in.rest.mortalidad;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avicontrol.AviControlModulo2Application;
import com.avicontrol.application.mortalidad.RegistrarMortalidadAnormalUseCase;
import com.avicontrol.application.mortalidad.result.RegistroMortalidadAnormalResult;
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
class MortalidadAnormalControllerTest {
  @Autowired MockMvc mvc;
  @MockitoBean RegistrarMortalidadAnormalUseCase caso;

  private static final String BODY =
      """
      {"fechaHoraEvento":"2026-11-20T10:00:00-05:00","cantidad":25,"causaProbable":"Respiratoria","observaciones":"Obs"}
      """;

  @Test
  void registraReporteAnormalConRolTrabajadorYRechazaNutricionista() throws Exception {
    UUID galponId = UUID.randomUUID();
    UUID alertaId = UUID.randomUUID();
    when(caso.registrar(any()))
        .thenReturn(
            new RegistroMortalidadAnormalResult(
                UUID.randomUUID(), galponId, new BigDecimal("2.50"), "ALTA", alertaId));

    mvc.perform(
            post("/api/galpones/{galponId}/mortalidad-anormal", galponId)
                .header("X-Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY)
                .with(SecurityMockMvcRequestPostProcessors.user("trab").roles("TRABAJADOR")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.alertaId").value(alertaId.toString()))
        .andExpect(jsonPath("$.severidad").value("ALTA"));

    mvc.perform(
            post("/api/galpones/{galponId}/mortalidad-anormal", galponId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY)
                .with(SecurityMockMvcRequestPostProcessors.user("nutri").roles("NUTRICIONISTA")))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
  }
}
