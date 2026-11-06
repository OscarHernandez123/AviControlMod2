package com.avicontrol.infrastructure.adapter.in.rest.sanitario.aislamiento.dto;

import com.avicontrol.domain.model.sanitario.aislamiento.EstadoSolicitudAislamiento;
import java.time.Instant;
import java.util.UUID;

public record SolicitudAislamientoResponse(
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
    Instant fechaDecision) {}
