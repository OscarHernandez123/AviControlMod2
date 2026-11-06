package com.avicontrol.domain.port.out.modulo1;

import java.util.Optional;
import java.util.UUID;

public interface GalponQueryPort {
  Optional<String> estadoVigente(UUID galponId);
}
