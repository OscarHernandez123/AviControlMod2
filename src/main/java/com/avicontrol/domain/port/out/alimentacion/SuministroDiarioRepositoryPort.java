package com.avicontrol.domain.port.out.alimentacion;

import com.avicontrol.domain.model.alimentacion.SuministroDiarioAlimento;
import java.util.Optional;
import java.util.UUID;

public interface SuministroDiarioRepositoryPort {
  void guardar(SuministroDiarioAlimento suministro);

  Optional<SuministroDiarioAlimento> buscarPorClave(UUID galponId, UUID clave);
}
