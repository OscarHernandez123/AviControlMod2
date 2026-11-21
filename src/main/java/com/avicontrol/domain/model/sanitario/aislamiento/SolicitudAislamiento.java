package com.avicontrol.domain.model.sanitario.aislamiento;

import com.avicontrol.domain.exception.sanitario.aislamiento.SolicitudAislamientoInvalidaException;
import com.avicontrol.domain.exception.sanitario.aislamiento.SolicitudAislamientoNoPendienteException;
import java.time.Instant;
import java.util.UUID;

public record SolicitudAislamiento(
    UUID id,
    UUID galponId,
    UUID loteId,
    String motivo,
    String signosClinicos,
    int poblacionVivaReportada,
    int edadDiasReportada,
    String trabajador,
    Instant fechaEmision,
    EstadoSolicitudAislamiento estado,
    String observaciones,
    String veterinario,
    Instant fechaDecision,
    long version) {

  public SolicitudAislamiento {
    if (id == null || galponId == null || loteId == null || fechaEmision == null || estado == null)
      throw new SolicitudAislamientoInvalidaException(
          "Identificadores, fecha y estado son obligatorios");
    motivo = texto(motivo, "motivo");
    signosClinicos = texto(signosClinicos, "signosClinicos");
    trabajador = texto(trabajador, "trabajador");
    if (poblacionVivaReportada <= 0 || edadDiasReportada <= 0 || version < 0)
      throw new SolicitudAislamientoInvalidaException("Población, edad o versión inválida");
    if (estado == EstadoSolicitudAislamiento.PENDIENTE_VALIDACION) {
      if (observaciones != null || veterinario != null || fechaDecision != null)
        throw new SolicitudAislamientoInvalidaException(
            "Una solicitud pendiente no tiene decisión");
    } else {
      observaciones = texto(observaciones, "observaciones");
      veterinario = texto(veterinario, "veterinario");
      if (fechaDecision == null)
        throw new SolicitudAislamientoInvalidaException("Fecha de decisión obligatoria");
    }
  }

  public SolicitudAislamiento decidir(
      DecisionAislamiento decision, String observaciones, String veterinario, Instant fecha) {
    if (estado != EstadoSolicitudAislamiento.PENDIENTE_VALIDACION)
      throw new SolicitudAislamientoNoPendienteException();
    if (decision == null) throw new SolicitudAislamientoInvalidaException("Decisión obligatoria");
    return new SolicitudAislamiento(
        id,
        galponId,
        loteId,
        motivo,
        signosClinicos,
        poblacionVivaReportada,
        edadDiasReportada,
        trabajador,
        fechaEmision,
        decision == DecisionAislamiento.ACEPTAR
            ? EstadoSolicitudAislamiento.ACEPTADA
            : EstadoSolicitudAislamiento.RECHAZADA,
        observaciones,
        veterinario,
        fecha,
        version + 1);
  }

  private static String texto(String value, String campo) {
    if (value == null || value.isBlank())
      throw new SolicitudAislamientoInvalidaException(campo + " es obligatorio");
    return value.strip();
  }
}
