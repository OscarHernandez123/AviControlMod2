package com.avicontrol.application.mortalidad.result;

import com.avicontrol.domain.model.mortalidad.RegistroMortalidadAnormal;
import java.math.BigDecimal;
import java.util.UUID;

public record RegistroMortalidadAnormalResult(
    UUID id, UUID galponId, BigDecimal porcentajeMortalidad, String severidad, UUID alertaId) {
  public static RegistroMortalidadAnormalResult de(RegistroMortalidadAnormal r, UUID alertaId) {
    return new RegistroMortalidadAnormalResult(
        r.id(), r.galponId(), r.porcentajeMortalidad(), r.severidad().name(), alertaId);
  }
}
