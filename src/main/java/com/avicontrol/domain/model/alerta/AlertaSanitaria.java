package com.avicontrol.domain.model.alerta;

import com.avicontrol.domain.model.mortalidad.RegistroMortalidadAnormal;
import java.time.Instant;
import java.util.UUID;

public record AlertaSanitaria(
    UUID id,
    UUID reporteId,
    UUID galponId,
    UUID loteId,
    String prioridad,
    String estado,
    Instant fechaEmision) {
  public static AlertaSanitaria de(RegistroMortalidadAnormal r, Instant ahora) {
    return new AlertaSanitaria(
        UUID.randomUUID(),
        r.id(),
        r.galponId(),
        r.loteId(),
        r.severidad().name(),
        "PENDIENTE_ATENCION_VETERINARIA",
        ahora);
  }
}
