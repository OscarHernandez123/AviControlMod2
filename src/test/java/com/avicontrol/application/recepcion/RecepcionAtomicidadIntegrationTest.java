package com.avicontrol.application.recepcion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avicontrol.AviControlModulo2Application;
import com.avicontrol.application.inventario.port.RegistrarEntradaInventarioPort;
import com.avicontrol.domain.model.catalogo.EtapaCrianza;
import com.avicontrol.domain.model.catalogo.UnidadMedidaMedicamento;
import java.math.BigDecimal;
import java.time.LocalDate;
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
class RecepcionAtomicidadIntegrationTest {
  private static final UUID PRODUCTO = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
  private static final LocalDate INGRESO = LocalDate.of(2026, 11, 5);

  @Autowired RegistrarRecepcionAlimentoUseCase alimento;
  @Autowired RegistrarRecepcionMedicamentoUseCase medicamento;
  @Autowired JdbcTemplate jdbc;
  @Autowired MockMvc mvc;
  @MockitoBean RegistrarEntradaInventarioPort inventario;

  @Test
  void revierteAlimentoSiInventarioFalla() {
    doThrow(new DataAccessResourceFailureException("Inventario no disponible"))
        .when(inventario)
        .alimento(any(), any(), any(), any());
    var command =
        new RegistrarRecepcionAlimentoCommand(
            PRODUCTO,
            EtapaCrianza.INICIO,
            "AL-ROLLBACK",
            INGRESO,
            INGRESO.plusDays(10),
            1,
            BigDecimal.ONE,
            BigDecimal.ONE,
            BigDecimal.ZERO,
            "admin");
    assertThrows(DataAccessResourceFailureException.class, () -> alimento.registrar(command));
    assertEquals(
        0,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM recepciones_alimento WHERE codigo_lote = 'AL-ROLLBACK'",
            Integer.class));
  }

  @Test
  void revierteMedicamentoSiInventarioFalla() {
    doThrow(new DataAccessResourceFailureException("Inventario no disponible"))
        .when(inventario)
        .medicamento(any(), any(), any(), any(), any());
    var command =
        new RegistrarRecepcionMedicamentoCommand(
            PRODUCTO,
            "MED-ROLLBACK",
            "Frasco",
            INGRESO,
            INGRESO.plusDays(10),
            1,
            BigDecimal.ONE,
            UnidadMedidaMedicamento.MILILITRO,
            BigDecimal.ONE,
            BigDecimal.ZERO,
            "admin");
    assertThrows(DataAccessResourceFailureException.class, () -> medicamento.registrar(command));
    assertEquals(
        0,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM recepciones_medicamento WHERE codigo_lote = 'MED-ROLLBACK'",
            Integer.class));
  }

  @Test
  void informa503SinPersistirRecepcion() throws Exception {
    doThrow(new DataAccessResourceFailureException("Inventario no disponible"))
        .when(inventario)
        .alimento(any(), any(), any(), any());
    String json =
        """
        {"alimentoId":"550e8400-e29b-41d4-a716-446655440000","tipoAlimento":"INICIO","codigoLote":"AL-503",
        "fechaIngreso":"2026-11-05","fechaVencimiento":"2027-03-05","cantidadBultos":1,
        "pesoPorBultoKg":40,"precioNetoPorBulto":82000,"impuestoPorcentaje":19}
        """;
    mvc.perform(
            post("/api/recepciones/alimentos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("PERSISTENCIA_NO_DISPONIBLE"));
    assertEquals(
        0,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM recepciones_alimento WHERE codigo_lote = 'AL-503'",
            Integer.class));
  }

  @Test
  void informa503ParaMedicamento() throws Exception {
    doThrow(new DataAccessResourceFailureException("Inventario no disponible"))
        .when(inventario)
        .medicamento(any(), any(), any(), any(), any());
    String json =
        """
        {"medicamentoId":"550e8400-e29b-41d4-a716-446655440000","codigoLote":"MED-503",
        "presentacion":"Frasco","fechaIngreso":"2026-11-05","fechaVencimiento":"2027-05-05",
        "cantidadPresentaciones":1,"contenidoPorPresentacion":500,"unidadContenido":"MILILITRO",
        "precioNetoPorPresentacion":100,"impuestoPorcentaje":0}
        """;
    mvc.perform(
            post("/api/recepciones/medicamentos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
        .andExpect(status().isServiceUnavailable());
    assertEquals(
        0,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM recepciones_medicamento WHERE codigo_lote = 'MED-503'",
            Integer.class));
  }
}
