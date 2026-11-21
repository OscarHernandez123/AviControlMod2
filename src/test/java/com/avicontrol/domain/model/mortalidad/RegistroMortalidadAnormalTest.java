package com.avicontrol.domain.model.mortalidad;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RegistroMortalidadAnormalTest {
  @Test
  void asignaSeveridadEnLosLimitesYEnMortalidadTotal() {
    assertEquals(NivelSeveridadMortalidad.ALTA, reporte(1, 5000).severidad());
    assertEquals(NivelSeveridadMortalidad.CRITICA, reporte(10, 1000).severidad());
    assertEquals(NivelSeveridadMortalidad.EMERGENCIA_SANITARIA, reporte(50, 1000).severidad());
    assertEquals(NivelSeveridadMortalidad.EMERGENCIA_SANITARIA, reporte(25, 25).severidad());
    assertEquals(
        0, reporte(30, 1000).porcentajeMortalidad().compareTo(new java.math.BigDecimal("3.0000")));
  }

  private RegistroMortalidadAnormal reporte(int cantidad, int poblacion) {
    Instant fecha = Instant.parse("2026-11-12T12:00:00Z");
    return RegistroMortalidadAnormal.crear(
        UUID.randomUUID(),
        null,
        UUID.randomUUID(),
        UUID.randomUUID(),
        fecha,
        fecha,
        cantidad,
        poblacion,
        "RESPIRATORIA",
        null,
        "operario");
  }
}
