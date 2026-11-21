package com.avicontrol.domain.port.out.sanitario.aislamiento;

import com.avicontrol.domain.model.sanitario.aislamiento.GalponLoteAislamiento;
import java.util.Optional;
import java.util.UUID;

public interface GalponLoteAislamientoQueryPort {
  Optional<GalponLoteAislamiento> consultar(UUID galponId);
}
