package com.avicontrol.domain.model.sacrificio.productivo;

import com.avicontrol.domain.exception.sacrificio.productivo.FechaProgramadaInvalidaException;
import com.avicontrol.domain.exception.sacrificio.productivo.OrdenNoModificableException;
import java.time.Instant;
import java.util.UUID;

public record OrdenSacrificioProductivo(
    UUID id,
    UUID galponId,
    UUID loteId,
    EstadoOrdenSacrificio estado,
    Instant fechaHoraProgramada,
    Instant fechaCreacion,
    Instant fechaReprogramacion,
    Instant fechaCancelacion,
    Instant fechaEjecucion,
    long version) {
  public static OrdenSacrificioProductivo programar(
      UUID galponId, UUID loteId, Instant fecha, Instant ahora) {
    validarFecha(fecha, ahora);
    return new OrdenSacrificioProductivo(
        UUID.randomUUID(),
        galponId,
        loteId,
        EstadoOrdenSacrificio.PROGRAMADA,
        fecha,
        ahora,
        null,
        null,
        null,
        0);
  }

  public OrdenSacrificioProductivo reprogramar(Instant fecha, Instant ahora) {
    exigirPendienteAntesDeFecha(ahora);
    validarFecha(fecha, ahora);
    return new OrdenSacrificioProductivo(
        id,
        galponId,
        loteId,
        EstadoOrdenSacrificio.REPROGRAMADA,
        fecha,
        fechaCreacion,
        ahora,
        fechaCancelacion,
        fechaEjecucion,
        version + 1);
  }

  public OrdenSacrificioProductivo cancelar(Instant ahora) {
    exigirPendienteAntesDeFecha(ahora);
    return new OrdenSacrificioProductivo(
        id,
        galponId,
        loteId,
        EstadoOrdenSacrificio.CANCELADA,
        fechaHoraProgramada,
        fechaCreacion,
        fechaReprogramacion,
        ahora,
        fechaEjecucion,
        version + 1);
  }

  public OrdenSacrificioProductivo ejecutar(Instant ahora) {
    exigirPendiente();
    if (fechaHoraProgramada.isAfter(ahora)) throw new OrdenNoModificableException();
    return new OrdenSacrificioProductivo(
        id,
        galponId,
        loteId,
        EstadoOrdenSacrificio.EJECUTADA,
        fechaHoraProgramada,
        fechaCreacion,
        fechaReprogramacion,
        fechaCancelacion,
        ahora,
        version + 1);
  }

  private void exigirPendiente() {
    if (!estado.pendiente()) throw new OrdenNoModificableException();
  }

  private void exigirPendienteAntesDeFecha(Instant ahora) {
    exigirPendiente();
    if (ahora == null || !fechaHoraProgramada.isAfter(ahora))
      throw new OrdenNoModificableException();
  }

  private static void validarFecha(Instant fecha, Instant ahora) {
    if (fecha == null || ahora == null || !fecha.isAfter(ahora))
      throw new FechaProgramadaInvalidaException();
  }
}
