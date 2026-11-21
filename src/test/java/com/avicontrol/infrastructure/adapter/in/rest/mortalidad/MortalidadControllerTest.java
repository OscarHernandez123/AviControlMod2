package com.avicontrol.infrastructure.adapter.in.rest.mortalidad;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avicontrol.AviControlModulo2Application;
import com.avicontrol.application.mortalidad.RegistrarMortalidadUseCase;
import com.avicontrol.application.mortalidad.result.RegistroMortalidadResult;
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
class MortalidadControllerTest {
  @Autowired MockMvc mvc;
  @MockitoBean RegistrarMortalidadUseCase caso;

  private static final String BODY =
      """
      {"fechaHoraEvento":"2026-11-20T10:00:00-05:00","cantidad":4,"causa":"ACCIDENTE","observaciones":"Obs"}
      """;

  @Test
  void registraMortalidadConRolTrabajadorYRechazaVeterinarioOSinAuth() throws Exception {
    UUID galponId = UUID.randomUUID();
    UUID id = UUID.randomUUID();
    when(caso.registrar(any()))
        .thenReturn(
            new RegistroMortalidadResult(
                id, galponId, UUID.randomUUID(), 4, "NORMAL", 996));

    mvc.perform(
            post("/api/galpones/{galponId}/mortalidad", galponId)
                .header("X-Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY)
                .with(SecurityMockMvcRequestPostProcessors.user("trab").roles("TRABAJADOR")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.poblacionResultante").value(996));

    mvc.perform(
            post("/api/galpones/{galponId}/mortalidad", galponId)
                .header("X-Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY)
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"));

    mvc.perform(
            post("/api/galpones/{galponId}/mortalidad", galponId)
                .header("X-Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void rechazaCantidadNoPositivaCon400ProblemJson() throws Exception {
    mvc.perform(
            post("/api/galpones/{galponId}/mortalidad", UUID.randomUUID())
                .header("X-Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fechaHoraEvento\":\"2026-11-20T10:00:00-05:00\",\"cantidad\":0,\"causa\":\"ACCIDENTE\"}")
                .with(SecurityMockMvcRequestPostProcessors.user("trab").roles("TRABAJADOR")))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"));
  }
}
