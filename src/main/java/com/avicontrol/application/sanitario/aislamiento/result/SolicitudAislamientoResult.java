package com.avicontrol.application.sanitario.aislamiento.result;

import com.avicontrol.domain.model.sanitario.aislamiento.EstadoSolicitudAislamiento;
import com.avicontrol.domain.model.sanitario.aislamiento.SolicitudAislamiento;
import java.time.Instant;
import java.util.UUID;

public record SolicitudAislamientoResult(
    UUID id,
    EstadoSolicitudAislamiento estado,
    UUID galponId,
    UUID loteId,
    String motivo,
    String signosClinicos,
    int poblacionVivaReportada,
    int edadDiasReportada,
    String trabajador,
    Instant fechaEmision,
    String observaciones,
    String veterinario,
    Instant fechaDecision) {
  public static SolicitudAislamientoResult de(SolicitudAislamiento s) {
    return new SolicitudAislamientoResult(
        s.id(),
        s.estado(),
        s.galponId(),
        s.loteId(),
        s.motivo(),
        s.signosClinicos(),
        s.poblacionVivaReportada(),
        s.edadDiasReportada(),
        s.trabajador(),
        s.fechaEmision(),
        s.observaciones(),
        s.veterinario(),
        s.fechaDecision());
  }
}
