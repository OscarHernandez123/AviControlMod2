package com.avicontrol.application.recepcion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avicontrol.AviControlModulo2Application;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = AviControlModulo2Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EdicionRecepcionAtomicidadIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @MockitoBean AuditoriaRecepcionPort auditoria;

  @Test
  void revierteRecepcionYEntradaSiAuditoriaFalla() throws Exception {
    String ruta = "/api/recepciones/alimentos";
    var admin = SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR");
    String location =
        mvc.perform(
                post(ruta)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
            {"alimentoId":"550e8400-e29b-41d4-a716-446655440000","tipoAlimento":"INICIO",
            "codigoLote":"AL-AUDIT-FAIL","fechaIngreso":"2026-11-05",
            "fechaVencimiento":"2027-03-05","cantidadBultos":1,"pesoPorBultoKg":40,
            "precioNetoPorBulto":100,"impuestoPorcentaje":0}
            """)
                    .with(admin))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getHeader("Location");
    UUID id = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
    doThrow(new DataAccessResourceFailureException("Auditoría no disponible"))
        .when(auditoria)
        .alimento(any(), any(), any());
    mvc.perform(
            put(ruta + "/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
            {"alimentoId":"550e8400-e29b-41d4-a716-446655440000",
            "fechaVencimiento":"2027-04-05","cantidadBultos":2,"pesoPorBultoKg":40,
            "precioNetoPorBulto":100,"impuestoPorcentaje":0}
            """)
                .with(admin))
        .andExpect(status().isServiceUnavailable());
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT cantidad_bultos FROM recepciones_alimento WHERE id = ?", Integer.class, id));
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM movimientos_inventario WHERE recepcion_id = ? AND cantidad = 40",
            Integer.class,
            id));
  }
}
