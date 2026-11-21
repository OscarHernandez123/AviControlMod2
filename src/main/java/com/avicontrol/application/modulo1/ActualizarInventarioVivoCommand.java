package com.avicontrol.application.modulo1;

import java.util.UUID;

public record ActualizarInventarioVivoCommand(
    UUID loteId,
    UUID galponId,
    int cantidadBajas,
    String causa,
    String actor,
    long versionEsperada,
    UUID idempotencyKey) {
  public ActualizarInventarioVivoCommand {
    if (loteId == null
        || galponId == null
        || cantidadBajas <= 0
        || causa == null
        || causa.isBlank()
        || actor == null
        || actor.isBlank()
        || versionEsperada < -1
        || idempotencyKey == null)
      throw new IllegalArgumentException("Descuento de población inválido");
  }
}
