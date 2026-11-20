package com.avicontrol.infrastructure.adapter.in.rest.sacrificio.productivo.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OrdenSacrificioResponse(
    UUID id,
    UUID galponId,
    UUID loteId,
    String estado,
    OffsetDateTime fechaHoraProgramada,
    com.avicontrol.domain.port.out.modulo1.EventosModulo1Port.Entrega integracion) {
  public OrdenSacrificioResponse(
      UUID id, UUID galponId, UUID loteId, String estado, OffsetDateTime fechaHoraProgramada) {
    this(id, galponId, loteId, estado, fechaHoraProgramada, null);
  }
}
