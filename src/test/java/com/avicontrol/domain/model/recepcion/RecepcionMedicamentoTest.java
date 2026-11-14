package com.avicontrol.domain.model.recepcion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.avicontrol.domain.exception.recepcion.CantidadInvalidaException;
import com.avicontrol.domain.exception.recepcion.UnidadIncompatibleException;
import com.avicontrol.domain.model.catalogo.UnidadMedidaMedicamento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecepcionMedicamentoTest {
  @Test
  void convierteMiligramosYValidaDimension() {
    var ingreso = LocalDate.of(2026, 11, 5);
    var r =
        RecepcionMedicamento.crear(
            UUID.randomUUID(),
            "Medicamento de prueba",
            "MED-1",
            "Caja",
            ingreso,
            ingreso.plusDays(30),
            20,
            new BigDecimal("500"),
            UnidadMedidaMedicamento.MILIGRAMO,
            new BigDecimal("45000"),
            new BigDecimal("19"),
            "admin");
    assertEquals(0, r.cantidadUnidadBase().compareTo(new BigDecimal("10")));
    assertEquals(UnidadMedidaMedicamento.GRAMO, r.unidadBase());
    var editada =
        r.editar(
            r.medicamentoId(),
            "Otro medicamento",
            ingreso.plusDays(40),
            2,
            new BigDecimal("500"),
            UnidadMedidaMedicamento.MILIGRAMO,
            new BigDecimal("100"),
            BigDecimal.ZERO);
    assertEquals(r.id(), editada.id());
    assertEquals(r.presentacion(), editada.presentacion());
    assertEquals(0, editada.cantidadUnidadBase().compareTo(BigDecimal.ONE));
    assertThrows(
        CantidadInvalidaException.class,
        () ->
            RecepcionMedicamento.crear(
                UUID.randomUUID(),
                "Medicamento",
                "MED-2",
                "Caja",
                ingreso,
                ingreso.plusDays(1),
                1,
                new BigDecimal("0.0001"),
                UnidadMedidaMedicamento.MILIGRAMO,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                "admin"));
    assertThrows(
        UnidadIncompatibleException.class,
        () ->
            UnidadMedidaMedicamento.MILIGRAMO.validarCompatibleCon(UnidadMedidaMedicamento.UNIDAD));
  }
}
