package com.avicontrol.domain.port.out.mortalidad;

import com.avicontrol.domain.model.mortalidad.RegistroMortalidadAnormal;
import java.util.Optional;
import java.util.UUID;

public interface RegistroMortalidadAnormalRepositoryPort {
  void bloquearGalpon(UUID galponId);

  void guardar(RegistroMortalidadAnormal reporte);

  Optional<RegistroMortalidadAnormal> buscar(UUID id);
}
