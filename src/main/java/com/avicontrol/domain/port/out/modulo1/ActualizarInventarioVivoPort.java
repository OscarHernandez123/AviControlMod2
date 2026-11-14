package com.avicontrol.domain.port.out.modulo1;

import java.util.UUID;

public interface ActualizarInventarioVivoPort {
  Resultado descontar(Descuento command);

  record Descuento(
      UUID loteId,
      UUID galponId,
      int cantidadBajas,
      String causa,
      String actor,
      long versionEsperada,
      UUID idempotencyKey) {}

  record Resultado(
      UUID loteId, int poblacionAnterior, int variacion, int poblacionResultante, String causa) {}
}
