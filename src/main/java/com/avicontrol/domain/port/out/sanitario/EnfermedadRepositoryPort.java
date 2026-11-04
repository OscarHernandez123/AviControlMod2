package com.avicontrol.domain.port.out.sanitario;

import com.avicontrol.domain.model.sanitario.enfermedad.Enfermedad;
import java.util.Optional;
import java.util.UUID;

public interface EnfermedadRepositoryPort {

  Enfermedad guardar(Enfermedad enfermedad);

  Optional<Enfermedad> buscarPorId(UUID enfermedadId);
}
