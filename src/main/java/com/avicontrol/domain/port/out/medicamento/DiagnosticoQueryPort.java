package com.avicontrol.domain.port.out.medicamento;

import com.avicontrol.domain.model.sanitario.diagnostico.DiagnosticoGalpon;
import java.util.Optional;
import java.util.UUID;

public interface DiagnosticoQueryPort {
  Optional<DiagnosticoGalpon> buscar(UUID diagnosticoId);
}
