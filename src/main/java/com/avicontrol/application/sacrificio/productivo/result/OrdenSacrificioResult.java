package com.avicontrol.application.sacrificio.productivo.result;

import com.avicontrol.domain.model.sacrificio.productivo.OrdenSacrificioProductivo;
import java.time.Instant;
import java.util.UUID;

public record OrdenSacrificioResult(
    UUID id, UUID galponId, UUID loteId, String estado, Instant fechaHoraProgramada) {
  public static OrdenSacrificioResult desde(OrdenSacrificioProductivo orden) {
    return new OrdenSacrificioResult(
        orden.id(),
        orden.galponId(),
        orden.loteId(),
        orden.estado().name(),
        orden.fechaHoraProgramada());
  }
}
