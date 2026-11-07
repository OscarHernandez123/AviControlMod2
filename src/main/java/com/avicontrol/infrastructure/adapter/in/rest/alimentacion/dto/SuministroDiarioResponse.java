package com.avicontrol.infrastructure.adapter.in.rest.alimentacion.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SuministroDiarioResponse(
    UUID id,
    UUID galponId,
    UUID loteId,
    UUID alimentoId,
    String tipoAlimento,
    BigDecimal cantidadKg,
    LocalDate fechaAplicacion,
    Instant fechaRegistro,
    String trabajador,
    UUID idempotencyKey,
    List<Detalle> detalles) {
  public record Detalle(
      UUID recepcionId, BigDecimal cantidadKg, BigDecimal precioNetoKg, String moneda) {}
}
