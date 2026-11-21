package com.avicontrol.infrastructure.adapter.in.rest.inventario;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avicontrol.AviControlModulo2Application;
import com.avicontrol.application.inventario.ConsultarCoberturaAlimentoUseCase;
import com.avicontrol.application.inventario.ConsultarInventarioUseCase;
import com.avicontrol.application.inventario.ConsultarResumenInventarioUseCase;
import com.avicontrol.domain.model.inventario.InventarioActual;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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
class InventarioControllerTest {
  @Autowired MockMvc mvc;
  @MockitoBean ConsultarInventarioUseCase inventario;
  @MockitoBean ConsultarResumenInventarioUseCase resumen;
  @MockitoBean ConsultarCoberturaAlimentoUseCase cobertura;

  @Test
  void permiteConsultarInventarioResumenYRequerimientoExclusivoAdministrador() throws Exception {
    UUID alimentoId = UUID.randomUUID();
    when(inventario.consultar())
        .thenReturn(new InventarioActual(List.of(), List.of(), Map.of(), Map.of()));
    when(resumen.consultar())
        .thenReturn(
            new ConsultarResumenInventarioUseCase.Resumen(
                new BigDecimal("10000"),
                new BigDecimal("2500"),
                new BigDecimal("25.00"),
                List.of()));
    when(cobertura.consultar(alimentoId))
        .thenReturn(
            new ConsultarCoberturaAlimentoUseCase.Cobertura(
                alimentoId,
                "Iniciador",
                "INICIO",
                new BigDecimal("500"),
                new BigDecimal("300"),
                "CUMPLE"));

    mvc.perform(
            get("/api/inventario")
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.alimentos").isArray());

    mvc.perform(
            get("/api/inventario/resumen")
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.porcentajeOcupacion").value(25.0));

    mvc.perform(
            get("/api/inventario/alimentos/{alimentoId}/requerimiento", alimentoId)
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("CUMPLE"));
  }

  @Test
  void rechazaOtrosRolesCon403YSinAutenticacionCon401() throws Exception {
    UUID alimentoId = UUID.randomUUID();
    for (String rol : new String[] {"VETERINARIO", "TRABAJADOR", "NUTRICIONISTA"}) {
      mvc.perform(
              get("/api/inventario")
                  .with(SecurityMockMvcRequestPostProcessors.user("u").roles(rol)))
          .andExpect(status().isForbidden())
          .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
          .andExpect(jsonPath("$.code").value("ACCESO_DENEGADO"));

      mvc.perform(
              get("/api/inventario/resumen")
                  .with(SecurityMockMvcRequestPostProcessors.user("u").roles(rol)))
          .andExpect(status().isForbidden());

      mvc.perform(
              get("/api/inventario/alimentos/{alimentoId}/requerimiento", alimentoId)
                  .with(SecurityMockMvcRequestPostProcessors.user("u").roles(rol)))
          .andExpect(status().isForbidden());
    }

    mvc.perform(get("/api/inventario"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
  }

  @Test
  void retorna404CuandoAlimentoNoExisteEnRequerimiento() throws Exception {
    UUID id = UUID.randomUUID();
    when(cobertura.consultar(id)).thenThrow(new NoSuchElementException("Alimento no encontrado"));

    mvc.perform(
            get("/api/inventario/alimentos/{alimentoId}/requerimiento", id)
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("RECURSO_NO_ENCONTRADO"));
  }
}
