package com.avicontrol.infrastructure.adapter.in.rest.sacrificio.sanitario.mapper;

import com.avicontrol.application.sacrificio.sanitario.result.OrdenSacrificioSanitarioResult;
import com.avicontrol.infrastructure.adapter.in.rest.sacrificio.sanitario.dto.OrdenSacrificioSanitarioResponse;
import org.springframework.stereotype.Component;

@Component
public class OrdenSacrificioSanitarioRestMapper {
  public OrdenSacrificioSanitarioResponse response(OrdenSacrificioSanitarioResult r) {
    return new OrdenSacrificioSanitarioResponse(
        r.id(), r.diagnosticoId(), r.galponId(), r.loteId(), r.estado(), r.poblacionSacrificada());
  }
}
