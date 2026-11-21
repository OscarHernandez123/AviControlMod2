package com.avicontrol.domain.port.out.modulo1;

import java.util.UUID;

public interface LoteUpdatePort {
  boolean finalizarAlojamiento(UUID galponId, UUID loteId);

  boolean sacrificarPoblacionSiPositiva(UUID galponId, UUID loteId);
}
