package com.avicontrol.domain.port.out.sanitario.medicacion;

import java.util.UUID;

public interface MedicamentoCatalogoQueryPort {
  boolean disponible(UUID id);
}
