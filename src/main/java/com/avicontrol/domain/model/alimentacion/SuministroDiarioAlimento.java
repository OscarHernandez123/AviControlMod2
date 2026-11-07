package com.avicontrol.domain.model.alimentacion;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SuministroDiarioAlimento(
    UUID id,
    UUID galponId,
    UUID loteId,
    UUID alimentoId,
    String tipoAlimento,
    BigDecimal cantidadKg,
    LocalDate fechaAplicacion,
    Instant fechaRegistro,
    String trabajador,
    UUID idempotenciaClave,
    List<Detalle> detalles) {
  public SuministroDiarioAlimento {
    if (id == null
        || galponId == null
        || loteId == null
        || alimentoId == null
        || tipoAlimento == null
        || tipoAlimento.isBlank()
        || cantidadKg == null
        || cantidadKg.signum() <= 0
        || cantidadKg.scale() > 4
        || fechaAplicacion == null
        || fechaRegistro == null
        || trabajador == null
        || trabajador.isBlank()
        || idempotenciaClave == null
        || detalles == null
        || detalles.isEmpty()) throw new IllegalArgumentException("Suministro incompleto");
    detalles = List.copyOf(detalles);
    BigDecimal total =
        detalles.stream().map(Detalle::cantidadKg).reduce(BigDecimal.ZERO, BigDecimal::add);
    if (total.compareTo(cantidadKg) != 0)
      throw new IllegalArgumentException("El detalle FEFO no cubre el suministro");
  }

  public record Detalle(UUID recepcionId, BigDecimal cantidadKg, BigDecimal precioNetoKg) {
    public Detalle {
      if (recepcionId == null
          || cantidadKg == null
          || cantidadKg.signum() <= 0
          || precioNetoKg == null
          || precioNetoKg.signum() < 0) throw new IllegalArgumentException("Detalle FEFO inválido");
    }
  }
}
