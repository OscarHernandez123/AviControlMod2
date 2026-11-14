package com.avicontrol.infrastructure.adapter.in.rest.sacrificio.productivo.mapper;

import com.avicontrol.application.sacrificio.productivo.result.ResultadoSacrificioResult;
import com.avicontrol.infrastructure.adapter.in.rest.sacrificio.productivo.dto.ResultadoSacrificioResponse;
import org.springframework.stereotype.Component;

@Component
public class ResultadoSacrificioRestMapper {
  public ResultadoSacrificioResponse response(ResultadoSacrificioResult result) {
    return new ResultadoSacrificioResponse(
        result.id(),
        result.ordenId(),
        result.galponId(),
        result.loteId(),
        result.cantidadFinalPollos(),
        result.pesoTotalKg(),
        result.estado(),
        result.fechaRegistro(),
        result.fechaCorreccion());
  }
}
