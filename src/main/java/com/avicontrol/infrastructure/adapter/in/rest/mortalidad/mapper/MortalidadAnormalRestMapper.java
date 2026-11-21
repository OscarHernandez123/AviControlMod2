package com.avicontrol.infrastructure.adapter.in.rest.mortalidad.mapper;

import com.avicontrol.application.mortalidad.result.RegistroMortalidadAnormalResult;
import com.avicontrol.infrastructure.adapter.in.rest.mortalidad.dto.RegistroMortalidadAnormalResponse;
import org.springframework.stereotype.Component;

@Component
public class MortalidadAnormalRestMapper {
  public RegistroMortalidadAnormalResponse response(RegistroMortalidadAnormalResult r) {
    return new RegistroMortalidadAnormalResponse(
        r.id(), r.galponId(), r.porcentajeMortalidad(), r.severidad(), r.alertaId());
  }
}
