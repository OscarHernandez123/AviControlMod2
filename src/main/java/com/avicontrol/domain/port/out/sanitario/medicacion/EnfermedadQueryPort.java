package com.avicontrol.domain.port.out.sanitario.medicacion;

import java.util.UUID;

public interface EnfermedadQueryPort {
  boolean disponible(UUID id);
}
