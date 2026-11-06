package com.avicontrol.infrastructure.adapter.in.rest.recepcion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avicontrol.AviControlModulo2Application;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = AviControlModulo2Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecepcionAlimentoControllerTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;

  @Test
  void creaEntradaYRestringeRol() throws Exception {
    String json =
        """
        {"alimentoId":"550e8400-e29b-41d4-a716-446655440000","tipoAlimento":"INICIO","codigoLote":"AL-2026-001",
        "fechaIngreso":"2026-11-05","fechaVencimiento":"2027-03-05","cantidadBultos":40,
        "pesoPorBultoKg":40,"precioNetoPorBulto":82000,"impuestoPorcentaje":19}
        """;
    mvc.perform(
            post("/api/recepciones/alimentos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.cantidadTotalKg").value(1600));
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM recepciones_alimento WHERE codigo_lote = 'AL-2026-001'",
            Integer.class));
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM movimientos_inventario WHERE tipo_producto = 'ALIMENTO' AND cantidad = 1600",
            Integer.class));
    mvc.perform(
            post("/api/recepciones/alimentos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(SecurityMockMvcRequestPostProcessors.user("trabajador").roles("TRABAJADOR")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESO_DENEGADO"));
    mvc.perform(
            post("/api/recepciones/alimentos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
  }

  @Test
  void validaCamposCatalogoYVencimiento() throws Exception {
    String json =
        """
        {"alimentoId":"550e8400-e29b-41d4-a716-446655440000","tipoAlimento":"INICIO","codigoLote":"AL-ERROR",
        "fechaIngreso":"2026-11-05","fechaVencimiento":"2027-03-05","cantidadBultos":0,
        "pesoPorBultoKg":40,"precioNetoPorBulto":82000,"impuestoPorcentaje":19}
        """;
    mvc.perform(
            post("/api/recepciones/alimentos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"));
    mvc.perform(
            post("/api/recepciones/alimentos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.replace(
                            "550e8400-e29b-41d4-a716-446655440000",
                            "550e8400-e29b-41d4-a716-446655440009")
                        .replace("\"cantidadBultos\":0", "\"cantidadBultos\":1"))
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
        .andExpect(status().isNotFound());
    mvc.perform(
            post("/api/recepciones/alimentos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.replace("\"cantidadBultos\":0", "\"cantidadBultos\":1")
                        .replace("2027-03-05", "2026-11-05"))
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
        .andExpect(status().isConflict());
    mvc.perform(
            post("/api/recepciones/alimentos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.replace("\"tipoAlimento\":\"INICIO\",", "")
                        .replace("\"cantidadBultos\":0", "\"cantidadBultos\":1"))
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
        .andExpect(status().isBadRequest());
  }
}
