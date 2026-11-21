package com.avicontrol.domain.port.out.sanitario.diagnostico;

import java.util.UUID;

public interface GalponReintegroPort {
  boolean reintegrarSiAislado(UUID galponId, UUID loteId);
}
