package com.avicontrol.infrastructure.adapter.in.rest.alimentacion;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avicontrol.AviControlModulo2Application;
import com.avicontrol.application.alimentacion.ProyeccionAlimentoUseCase;
import java.time.Instant;
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
class ProyeccionAlimentoControllerTest {
  @Autowired MockMvc mvc;
  @MockitoBean ProyeccionAlimentoUseCase useCase;

  @Test
  void validaSegregacionDeRolesParaRequerimientoBalanceYCompletarCosto() throws Exception {
    UUID loteId = UUID.randomUUID();
    when(useCase.consultarLote(loteId))
        .thenReturn(
            new ProyeccionAlimentoUseCase.Requerimiento(
                "L-01", loteId, UUID.randomUUID(), "ACTIVO", Instant.now(), List.of()));
    when(useCase.balance()).thenReturn(List.of());

    mvc.perform(
            get("/api/lotes/{loteId}/alimento-requerido", loteId)
                .with(SecurityMockMvcRequestPostProcessors.user("nutri").roles("NUTRICIONISTA")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.codigoLote").value("L-01"));

    mvc.perform(
            get("/api/lotes/{loteId}/alimento-requerido", loteId)
                .with(SecurityMockMvcRequestPostProcessors.user("trab").roles("TRABAJADOR")))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"));

    mvc.perform(
            get("/api/administracion/balance-abastecimiento")
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.alimentos").isArray());

    mvc.perform(
            post("/api/proyecciones/{id}/completar-costo", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"costoUnitarioKg\":2500,\"justificacion\":\"Factura\"}")
                .with(SecurityMockMvcRequestPostProcessors.user("nutri").roles("NUTRICIONISTA")))
        .andExpect(status().isForbidden());
  }

  @Test
  void retorna404CuandoLoteNoTieneProyeccion() throws Exception {
    UUID loteId = UUID.randomUUID();
    when(useCase.consultarLote(loteId)).thenThrow(new NoSuchElementException("Lote sin plan"));

    mvc.perform(
            get("/api/lotes/{loteId}/alimento-requerido", loteId)
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
  }
}
