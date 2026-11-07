package com.avicontrol.domain.port.out.sacrificio.sanitario;

import java.util.Optional;
import java.util.UUID;

public interface DiagnosticoSanitarioQueryPort {
  Optional<DiagnosticoDatos> activoPorGalpon(UUID galponId);

  record DiagnosticoDatos(
      UUID id, UUID galponId, UUID loteId, boolean requiereSacrificioSanitario) {}
}
