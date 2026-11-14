package com.avicontrol.domain.port.out.alerta;

import com.avicontrol.domain.model.alerta.AlertaSanitaria;
import java.util.Optional;
import java.util.UUID;

public interface AlertaSanitariaRepositoryPort {
  Optional<UUID> porReporte(UUID reporteId);

  void guardar(AlertaSanitaria alerta);
}
