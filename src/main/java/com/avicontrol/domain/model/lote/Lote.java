package com.avicontrol.domain.model.lote;

import com.avicontrol.domain.exception.galpon.FechaIngresoInconsistenteException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public record Lote(UUID id, UUID galponId, int poblacionActual, LocalDate fechaIngreso) {
  public Lote {
    if (id == null || galponId == null || poblacionActual < 0)
      throw new IllegalArgumentException("Datos del lote inconsistentes");
  }

  /** Cuenta el día de ingreso como día uno, usando fechas de la zona de negocio. */
  public Edad calcularEdad(LocalDate fechaConsulta) {
    if (fechaIngreso == null || fechaConsulta == null || fechaIngreso.isAfter(fechaConsulta))
      throw new FechaIngresoInconsistenteException();
    return Edad.deDias(ChronoUnit.DAYS.between(fechaIngreso, fechaConsulta) + 1);
  }
}
