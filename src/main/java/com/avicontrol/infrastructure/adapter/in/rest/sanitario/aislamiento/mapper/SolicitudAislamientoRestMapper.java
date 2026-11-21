package com.avicontrol.infrastructure.adapter.in.rest.sanitario.aislamiento.mapper;

import com.avicontrol.application.sanitario.aislamiento.CrearSolicitudAislamientoCommand;
import com.avicontrol.application.sanitario.aislamiento.result.SolicitudAislamientoResult;
import com.avicontrol.infrastructure.adapter.in.rest.sanitario.aislamiento.dto.CrearSolicitudAislamientoRequest;
import com.avicontrol.infrastructure.adapter.in.rest.sanitario.aislamiento.dto.SolicitudAislamientoResponse;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SolicitudAislamientoRestMapper {
  public CrearSolicitudAislamientoCommand command(
      UUID galponId, CrearSolicitudAislamientoRequest request, String actor) {
    return new CrearSolicitudAislamientoCommand(
        galponId, request.motivo(), request.signosClinicos(), actor);
  }

  public SolicitudAislamientoResponse response(SolicitudAislamientoResult result) {
    return new SolicitudAislamientoResponse(
        result.id(),
        result.estado(),
        result.galponId(),
        result.loteId(),
        result.motivo(),
        result.signosClinicos(),
        result.poblacionVivaReportada(),
        result.edadDiasReportada(),
        result.trabajador(),
        result.fechaEmision(),
        result.observaciones(),
        result.veterinario(),
        result.fechaDecision());
  }
}
