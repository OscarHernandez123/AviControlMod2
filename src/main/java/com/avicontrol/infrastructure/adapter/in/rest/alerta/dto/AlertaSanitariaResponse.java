package com.avicontrol.infrastructure.adapter.in.rest.alerta.dto;

import com.avicontrol.application.alerta.result.AlertaSanitariaResult;
import java.math.BigDecimal;
import java.util.UUID;

public record AlertaSanitariaResponse(
    UUID id,
    String prioridad,
    String estado,
    UUID galponId,
    UUID loteId,
    String nombreGalpon,
    Integer aforoMaximo,
    String estadoGalpon,
    Integer poblacionActual,
    Long edadDias,
    int cantidadMuertes,
    BigDecimal porcentajeMortalidad,
    String causaProbable,
    String observaciones,
    String responsable) {
  public static AlertaSanitariaResponse de(AlertaSanitariaResult r) {
    return new AlertaSanitariaResponse(
        r.id(),
        r.prioridad(),
        r.estado(),
        r.galponId(),
        r.loteId(),
        r.nombreGalpon(),
        r.aforoMaximo(),
        r.estadoGalpon(),
        r.poblacionActual(),
        r.edadDias(),
        r.cantidadMuertes(),
        r.porcentajeMortalidad(),
        r.causaProbable(),
        r.observaciones(),
        r.responsable());
  }
}
