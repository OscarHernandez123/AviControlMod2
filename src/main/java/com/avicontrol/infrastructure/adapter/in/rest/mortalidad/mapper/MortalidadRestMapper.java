package com.avicontrol.infrastructure.adapter.in.rest.mortalidad.mapper;

import com.avicontrol.application.mortalidad.result.RegistroMortalidadResult;
import com.avicontrol.infrastructure.adapter.in.rest.mortalidad.dto.RegistroMortalidadResponse;
import org.springframework.stereotype.Component;

@Component
public class MortalidadRestMapper {
  public RegistroMortalidadResponse response(RegistroMortalidadResult r) {
    return new RegistroMortalidadResponse(
        r.id(), r.galponId(), r.loteId(), r.cantidad(), r.clasificacion(), r.poblacionResultante());
  }
}
