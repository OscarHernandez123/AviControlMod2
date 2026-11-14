package com.avicontrol.domain.model.mortalidad;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

public record RegistroMortalidadAnormal(
    UUID id,
    UUID mortalidadId,
    UUID galponId,
    UUID loteId,
    Instant fechaHoraEvento,
    Instant fechaHoraRegistro,
    int cantidad,
    int poblacionBase,
    BigDecimal porcentajeMortalidad,
    NivelSeveridadMortalidad severidad,
    String causaProbable,
    String observaciones,
    String responsable) {
  public static RegistroMortalidadAnormal crear(
      UUID id,
      UUID mortalidadId,
      UUID galponId,
      UUID loteId,
      Instant fecha,
      Instant ahora,
      int cantidad,
      int poblacion,
      String causa,
      String observaciones,
      String responsable) {
    if (id == null
        || galponId == null
        || loteId == null
        || fecha == null
        || ahora == null
        || cantidad <= 0
        || poblacion <= 0
        || cantidad > poblacion
        || causa == null
        || causa.isBlank()
        || responsable == null
        || responsable.isBlank()) throw new IllegalArgumentException("Mortalidad anormal inválida");
    BigDecimal porcentaje =
        BigDecimal.valueOf(cantidad)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(poblacion), 4, RoundingMode.HALF_UP);
    var severidad =
        cantidad == poblacion || porcentaje.compareTo(BigDecimal.valueOf(5)) >= 0
            ? NivelSeveridadMortalidad.EMERGENCIA_SANITARIA
            : porcentaje.compareTo(BigDecimal.ONE) >= 0
                ? NivelSeveridadMortalidad.CRITICA
                : NivelSeveridadMortalidad.ALTA;
    return new RegistroMortalidadAnormal(
        id,
        mortalidadId,
        galponId,
        loteId,
        fecha,
        ahora,
        cantidad,
        poblacion,
        porcentaje,
        severidad,
        causa.strip(),
        observaciones,
        responsable);
  }
}
