package com.avicontrol.infrastructure.adapter.in.rest.sanitario.aislamiento.mapper;

import com.avicontrol.application.sanitario.aislamiento.ValidarAislamientoCommand;
import com.avicontrol.application.sanitario.aislamiento.result.ValidacionAislamientoResult;
import com.avicontrol.infrastructure.adapter.in.rest.sanitario.aislamiento.dto.ValidacionAislamientoResponse;
import com.avicontrol.infrastructure.adapter.in.rest.sanitario.aislamiento.dto.ValidarAislamientoRequest;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AislamientoRestMapper {
  public ValidarAislamientoCommand command(
      UUID id, ValidarAislamientoRequest request, String actor) {
    return new ValidarAislamientoCommand(id, request.decision(), request.observaciones(), actor);
  }

  public ValidacionAislamientoResponse response(ValidacionAislamientoResult result) {
    return new ValidacionAislamientoResponse(
        result.id(), result.estado(), result.galponId(), result.estadoGalponResultante());
  }
}
