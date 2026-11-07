package com.avicontrol.application.sacrificio.sanitario.result;

import com.avicontrol.domain.model.sacrificio.sanitario.OrdenSacrificioSanitario;
import java.util.UUID;

public record OrdenSacrificioSanitarioResult(
    UUID id,
    UUID diagnosticoId,
    UUID galponId,
    UUID loteId,
    String estado,
    Integer poblacionSacrificada) {
  public static OrdenSacrificioSanitarioResult de(OrdenSacrificioSanitario o) {
    return new OrdenSacrificioSanitarioResult(
        o.id(),
        o.diagnosticoId(),
        o.galponId(),
        o.loteId(),
        o.estado().name(),
        o.poblacionSacrificada());
  }
}
