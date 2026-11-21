package com.avicontrol.domain.model.sanitario.medicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ProgresoTratamientoTest {
  @Test
  void calculaDiaDosDeCincoYExcluyeFechasFueraDelTratamiento() {
    LocalDate inicio = LocalDate.of(2026, 11, 5);
    assertEquals(
        "Día 2 de 5",
        ProgresoTratamiento.calcular(inicio, 5, inicio.plusDays(1)).orElseThrow().etiqueta());
    assertTrue(ProgresoTratamiento.calcular(inicio, 5, inicio.minusDays(1)).isEmpty());
    assertTrue(ProgresoTratamiento.calcular(inicio, 5, inicio.plusDays(5)).isEmpty());
  }
}
