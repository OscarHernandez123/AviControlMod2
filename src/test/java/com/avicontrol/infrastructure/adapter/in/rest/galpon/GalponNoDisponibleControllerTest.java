package com.avicontrol.infrastructure.adapter.in.rest.galpon;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avicontrol.AviControlModulo2Application;
import com.avicontrol.domain.exception.galpon.InformacionGalponNoDisponibleException;
import com.avicontrol.domain.port.out.modulo1.GalponQueryPort;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = AviControlModulo2Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GalponNoDisponibleControllerTest {
  @DynamicPropertySource
  static void baseAislada(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.datasource.url",
        () ->
            "jdbc:h2:mem:galpon_no_disponible_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
  }

  @Autowired MockMvc mvc;
  @MockitoBean GalponQueryPort galpones;

  @Test
  void responde503EnLasCuatroConsultas() throws Exception {
    var indisponible = new InformacionGalponNoDisponibleException();
    when(galpones.consultar(any(UUID.class))).thenThrow(indisponible);
    when(galpones.todos()).thenThrow(indisponible);
    when(galpones.listar(anyInt(), anyInt(), anyString(), anyBoolean())).thenThrow(indisponible);
    UUID id = UUID.randomUUID();
    mvc.perform(get("/api/galpones/{id}/edad-lote", id).with(admin()))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("MODULO1_NO_DISPONIBLE"));
    mvc.perform(get("/api/galpones/{id}", id).with(admin()))
        .andExpect(status().isServiceUnavailable());
    mvc.perform(get("/api/galpones/resumen").with(admin()))
        .andExpect(status().isServiceUnavailable());
    mvc.perform(get("/api/galpones").with(admin())).andExpect(status().isServiceUnavailable());
  }

  private SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor admin() {
    return SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR");
  }
}
