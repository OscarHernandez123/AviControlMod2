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
class RecepcionMedicamentoControllerTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;

  @Test
  void creaEntradaNormalizada() throws Exception {
    String json =
        """
        {"medicamentoId":"550e8400-e29b-41d4-a716-446655440000","codigoLote":"MED-2026-001",
        "presentacion":"Frasco","fechaIngreso":"2026-11-05","fechaVencimiento":"2027-05-05",
        "cantidadPresentaciones":20,"contenidoPorPresentacion":500,"unidadContenido":"MILILITRO",
        "precioNetoPorPresentacion":45000,"impuestoPorcentaje":19}
        """;
    mvc.perform(
            post("/api/recepciones/medicamentos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.cantidadUnidadBase").value(10000));
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM recepciones_medicamento WHERE codigo_lote = 'MED-2026-001'",
            Integer.class));
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM movimientos_inventario WHERE tipo_producto = 'MEDICAMENTO' AND cantidad = 10000 AND unidad = 'MILILITRO'",
            Integer.class));
  }

  @Test
  void rechazaUnidadIncompatible() throws Exception {
    String json =
        """
        {"medicamentoId":"550e8400-e29b-41d4-a716-446655440000","codigoLote":"MED-ERROR",
        "presentacion":"Caja","fechaIngreso":"2026-11-05","fechaVencimiento":"2027-05-05",
        "cantidadPresentaciones":1,"contenidoPorPresentacion":1,"unidadContenido":"UNIDAD",
        "precioNetoPorPresentacion":100,"impuestoPorcentaje":0}
        """;
    mvc.perform(
            post("/api/recepciones/medicamentos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONFLICTO_RECEPCION"));
    assertEquals(
        0,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM recepciones_medicamento WHERE codigo_lote = 'MED-ERROR'",
            Integer.class));
    mvc.perform(
            post("/api/recepciones/medicamentos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(SecurityMockMvcRequestPostProcessors.user("trabajador").roles("TRABAJADOR")))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/recepciones/medicamentos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.replace(
                        "550e8400-e29b-41d4-a716-446655440000",
                        "550e8400-e29b-41d4-a716-446655440009"))
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
        .andExpect(status().isNotFound());
    mvc.perform(
            post("/api/recepciones/medicamentos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.replace("\"cantidadPresentaciones\":1", "\"cantidadPresentaciones\":0"))
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
        .andExpect(status().isBadRequest());
  }
}
