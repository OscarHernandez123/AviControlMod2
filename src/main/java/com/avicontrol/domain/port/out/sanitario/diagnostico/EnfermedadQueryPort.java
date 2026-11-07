package com.avicontrol.domain.port.out.sanitario.diagnostico;

import com.avicontrol.domain.model.sanitario.enfermedad.Enfermedad;
import java.util.Optional;
import java.util.UUID;

public interface EnfermedadQueryPort {
  Optional<Enfermedad> buscarEnfermedad(UUID id);
}
