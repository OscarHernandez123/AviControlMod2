package com.avicontrol.domain.port.out.modulo1;

import java.util.UUID;

public interface GalponUpdatePort {
  boolean aislarSiProductiva(UUID galponId);

  boolean pasarACosechaSiProductiva(UUID galponId, UUID loteId);

  boolean vaciarSiEnCosecha(UUID galponId, UUID loteId);

  boolean vaciarSiAislado(UUID galponId, UUID loteId);
}
