package com.avicontrol.domain.port.out.sanitario.enfermedad;

import com.avicontrol.domain.model.sanitario.enfermedad.Enfermedad;
import java.util.Optional;
import java.util.UUID;

public interface EnfermedadRepositoryPort {
  void crear(Enfermedad enfermedad);

  Optional<Enfermedad> buscar(UUID id);

  boolean actualizar(Enfermedad anterior, Enfermedad actualizada);
}
