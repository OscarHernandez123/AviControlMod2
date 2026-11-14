package com.avicontrol.domain.model.sanitario.medicacion;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public record ProgresoTratamiento(int diaActual, int duracionDias) {
  public ProgresoTratamiento {
    if (diaActual < 1 || duracionDias < 1 || diaActual > duracionDias)
      throw new IllegalArgumentException("Progreso del tratamiento inválido");
  }

  public static Optional<ProgresoTratamiento> calcular(
      LocalDate inicio, int duracionDias, LocalDate fecha) {
    if (inicio == null || fecha == null || duracionDias < 1)
      throw new IllegalArgumentException("Fechas o duración inválidas");
    long dia = ChronoUnit.DAYS.between(inicio, fecha) + 1;
    if (dia < 1 || dia > duracionDias) return Optional.empty();
    return Optional.of(new ProgresoTratamiento(Math.toIntExact(dia), duracionDias));
  }

  public String etiqueta() {
    return "Día " + diaActual + " de " + duracionDias;
  }
}
