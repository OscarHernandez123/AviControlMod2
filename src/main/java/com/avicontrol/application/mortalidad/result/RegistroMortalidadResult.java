package com.avicontrol.application.mortalidad.result;

import com.avicontrol.domain.model.mortalidad.RegistroMortalidad;
import java.util.UUID;

public record RegistroMortalidadResult(
    UUID id,
    UUID galponId,
    UUID loteId,
    int cantidad,
    String clasificacion,
    int poblacionResultante) {
  public static RegistroMortalidadResult de(RegistroMortalidad r) {
    return new RegistroMortalidadResult(
        r.id(),
        r.galponId(),
        r.loteId(),
        r.cantidad(),
        r.clasificacion().name(),
        r.poblacionResultante());
  }
}
