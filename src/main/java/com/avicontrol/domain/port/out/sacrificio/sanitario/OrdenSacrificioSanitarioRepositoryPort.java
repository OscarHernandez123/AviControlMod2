package com.avicontrol.domain.port.out.sacrificio.sanitario;

import com.avicontrol.domain.model.sacrificio.sanitario.OrdenSacrificioSanitario;
import java.util.Optional;
import java.util.UUID;

public interface OrdenSacrificioSanitarioRepositoryPort {
  void crear(OrdenSacrificioSanitario orden);

  Optional<OrdenSacrificioSanitario> bloquear(UUID id);

  boolean ejecutar(OrdenSacrificioSanitario orden);
}
