package com.avicontrol.domain.model.lote;

import com.avicontrol.domain.exception.galpon.FechaIngresoInconsistenteException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

public record Lote(
    UUID id,
    String nombre,
    int poblacionInicial,
    int poblacionActual,
    LocalDate fechaIngreso,
    BigDecimal costoTotal,
    UUID galponId) {

  public Lote {
    Objects.requireNonNull(id, "El id del lote es obligatorio");
    if (nombre == null || nombre.isBlank()) {
      throw new IllegalArgumentException("El nombre del lote es obligatorio");
    }
    if (poblacionInicial < 0 || poblacionActual < 0 || poblacionActual > poblacionInicial) {
      throw new IllegalArgumentException("La población del lote no es válida");
    }
    Objects.requireNonNull(fechaIngreso, "La fecha de ingreso es obligatoria");
    Objects.requireNonNull(costoTotal, "El costo total es obligatorio");
    if (costoTotal.signum() < 0) {
      throw new IllegalArgumentException("El costo total no puede ser negativo");
    }
    Objects.requireNonNull(galponId, "El id del galpón es obligatorio");
  }

  public Edad calcularEdad(LocalDate fechaConsulta) {
    Objects.requireNonNull(fechaConsulta, "La fecha de consulta es obligatoria");
    if (fechaIngreso.isAfter(fechaConsulta)) {
      throw new FechaIngresoInconsistenteException();
    }
    return Edad.fromDiasTotales(
        Math.toIntExact(ChronoUnit.DAYS.between(fechaIngreso, fechaConsulta) + 1));
  }
}
