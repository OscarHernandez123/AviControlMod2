package com.avicontrol.infrastructure.adapter.in.rest.alerta;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avicontrol.AviControlModulo2Application;
import com.avicontrol.application.alerta.ConsultarAlertasSanitariasUseCase;
import com.avicontrol.application.alerta.result.AlertaSanitariaResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = AviControlModulo2Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AlertaSanitariaControllerTest {
  @Autowired MockMvc mvc;
  @MockitoBean ConsultarAlertasSanitariasUseCase caso;

  @Test
  void listaYDetallaConVeterinarioOAdministradorYRechazaOtrosRoles() throws Exception {
    UUID id = UUID.randomUUID();
    var item =
        new AlertaSanitariaResult(
            id,
            "ALTA",
            "PENDIENTE_ATENCION_VETERINARIA",
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Galpón 1",
            1500,
            "EN_PRODUCCION",
            980,
            12L,
            20,
            new BigDecimal("2.00"),
            "Respiratoria",
            "Obs",
            "trabajador");

    when(caso.listar("PENDIENTE_ATENCION_VETERINARIA", 0, 20))
        .thenReturn(new ConsultarAlertasSanitariasUseCase.PaginaAlertas(List.of(item), 1L, 0, 20));
    when(caso.detalle(id)).thenReturn(item);

    mvc.perform(
            get("/api/alertas-sanitarias")
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElementos").value(1))
        .andExpect(jsonPath("$.elementos[0].id").value(id.toString()));

    mvc.perform(
            get("/api/alertas-sanitarias/{alertaId}", id)
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombreGalpon").value("Galpón 1"));

    mvc.perform(
            get("/api/alertas-sanitarias")
                .with(SecurityMockMvcRequestPostProcessors.user("trab").roles("TRABAJADOR")))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("ACCESO_DENEGADO"));

    mvc.perform(get("/api/alertas-sanitarias"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
  }

  @Test
  void retorna404ProblemJsonCuandoAlertaNoExiste() throws Exception {
    UUID id = UUID.randomUUID();
    when(caso.detalle(id)).thenThrow(new NoSuchElementException("Alerta no encontrada"));

    mvc.perform(
            get("/api/alertas-sanitarias/{alertaId}", id)
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("RECURSO_NO_ENCONTRADO"));
  }
}
