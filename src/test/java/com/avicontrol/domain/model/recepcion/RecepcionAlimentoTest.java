package com.avicontrol.domain.model.recepcion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.avicontrol.domain.exception.recepcion.CantidadInvalidaException;
import com.avicontrol.domain.exception.recepcion.FechaVencimientoInvalidaException;
import com.avicontrol.domain.model.catalogo.EtapaCrianza;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecepcionAlimentoTest {
  @Test
  void calculaTotalesYRechazaVencimiento() {
    UUID id = UUID.randomUUID();
    LocalDate ingreso = LocalDate.of(2026, 11, 5);
    RecepcionAlimento r =
        RecepcionAlimento.crear(
            id,
            "Alimento de prueba",
            EtapaCrianza.INICIO,
            "AL-1",
            ingreso,
            ingreso.plusDays(30),
            40,
            new BigDecimal("40"),
            new BigDecimal("82000"),
            new BigDecimal("19"),
            "admin");
    assertEquals(0, r.cantidadTotalKg().compareTo(new BigDecimal("1600")));
    assertEquals(0, r.precioNetoTotal().compareTo(new BigDecimal("3280000")));
    assertEquals(0, r.valorImpuesto().compareTo(new BigDecimal("623200")));
    var editada =
        r.editar(
            id,
            "Otro alimento",
            ingreso.plusDays(40),
            2,
            new BigDecimal("20"),
            new BigDecimal("100"),
            BigDecimal.ZERO);
    assertEquals(r.id(), editada.id());
    assertEquals(r.tipoAlimento(), editada.tipoAlimento());
    assertEquals(r.codigoLote(), editada.codigoLote());
    assertEquals(0, editada.cantidadTotalKg().compareTo(new BigDecimal("40")));
    assertThrows(
        CantidadInvalidaException.class,
        () ->
            RecepcionAlimento.crear(
                id,
                "Alimento",
                EtapaCrianza.INICIO,
                "AL-2",
                ingreso,
                ingreso.plusDays(1),
                2,
                new BigDecimal("99999999999999"),
                BigDecimal.ONE,
                BigDecimal.ZERO,
                "admin"));
    assertThrows(
        FechaVencimientoInvalidaException.class,
        () ->
            RecepcionAlimento.crear(
                id,
                "Alimento de prueba",
                EtapaCrianza.INICIO,
                "AL-1",
                ingreso,
                ingreso,
                40,
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                "admin"));
  }
}
