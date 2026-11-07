package com.avicontrol.infrastructure.adapter.in.rest.sacrificio.productivo.mapper;

import com.avicontrol.application.sacrificio.productivo.result.ValidacionAptitudResult;
import com.avicontrol.infrastructure.adapter.in.rest.sacrificio.productivo.dto.ValidacionAptitudSacrificioResponse;
import org.springframework.stereotype.Component;

@Component
public class AptitudSacrificioRestMapper {
  public ValidacionAptitudSacrificioResponse response(ValidacionAptitudResult result) {
    return new ValidacionAptitudSacrificioResponse(
        result.galponId(),
        result.loteId(),
        result.apto(),
        result.motivosNoAptitud(),
        result.estadoGalponResultante());
  }
}
