package com.avicontrol.domain.port.out.mortalidad;

import com.avicontrol.domain.model.mortalidad.RegistroMortalidad;
import java.util.Optional;
import java.util.UUID;

public interface RegistroMortalidadRepositoryPort {
  void guardar(RegistroMortalidad registro);

  Optional<RegistroMortalidad> buscar(UUID id);

  int totalDia(UUID galponId, UUID loteId, java.time.Instant inicio, java.time.Instant fin);
}
