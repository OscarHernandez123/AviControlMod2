package com.avicontrol.domain.model.sacrificio.sanitario;

import com.avicontrol.domain.exception.sacrificio.sanitario.OrdenSanitariaNoPendienteException;
import java.time.Instant;
import java.util.UUID;

public record OrdenSacrificioSanitario(
    UUID id,
    UUID diagnosticoId,
    UUID galponId,
    UUID loteId,
    EstadoOrdenSacrificioSanitario estado,
    Integer poblacionSacrificada,
    Instant fechaEmision,
    Instant fechaEjecucion,
    long version) {
  public OrdenSacrificioSanitario {
    if (id == null
        || diagnosticoId == null
        || galponId == null
        || loteId == null
        || estado == null
        || fechaEmision == null
        || version < 0
        || (estado == EstadoOrdenSacrificioSanitario.PENDIENTE
            ? poblacionSacrificada != null || fechaEjecucion != null
            : poblacionSacrificada == null || poblacionSacrificada <= 0 || fechaEjecucion == null))
      throw new IllegalArgumentException("Orden sanitaria inconsistente");
  }

  public static OrdenSacrificioSanitario emitir(
      UUID diagnosticoId, UUID galponId, UUID loteId, Instant ahora) {
    return new OrdenSacrificioSanitario(
        UUID.randomUUID(),
        diagnosticoId,
        galponId,
        loteId,
        EstadoOrdenSacrificioSanitario.PENDIENTE,
        null,
        ahora,
        null,
        0);
  }

  public OrdenSacrificioSanitario ejecutar(int poblacion, Instant ahora) {
    if (estado != EstadoOrdenSacrificioSanitario.PENDIENTE)
      throw new OrdenSanitariaNoPendienteException();
    if (poblacion <= 0)
      throw new com.avicontrol.domain.exception.sacrificio.sanitario
          .PoblacionNoSacrificableException();
    return new OrdenSacrificioSanitario(
        id,
        diagnosticoId,
        galponId,
        loteId,
        EstadoOrdenSacrificioSanitario.EJECUTADA,
        poblacion,
        fechaEmision,
        ahora,
        version + 1);
  }
}
