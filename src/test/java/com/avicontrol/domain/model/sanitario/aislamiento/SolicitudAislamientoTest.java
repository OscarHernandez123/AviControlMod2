package com.avicontrol.domain.model.sanitario.aislamiento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.avicontrol.domain.exception.sanitario.aislamiento.SolicitudAislamientoInvalidaException;
import com.avicontrol.domain.exception.sanitario.aislamiento.SolicitudAislamientoNoPendienteException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SolicitudAislamientoTest {
  private SolicitudAislamiento pendiente() {
    return new SolicitudAislamiento(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "Tos",
        "Decaimiento",
        8000,
        5,
        "trabajador",
        Instant.now(),
        EstadoSolicitudAislamiento.PENDIENTE_VALIDACION,
        null,
        null,
        null,
        0);
  }

  @Test
  void conservaSnapshotYPermiteUnaDecision() {
    SolicitudAislamiento original = pendiente();
    SolicitudAislamiento resuelta =
        original.decidir(DecisionAislamiento.ACEPTAR, "Aislar", "veterinario", Instant.now());
    assertEquals(EstadoSolicitudAislamiento.PENDIENTE_VALIDACION, original.estado());
    assertEquals(EstadoSolicitudAislamiento.ACEPTADA, resuelta.estado());
    assertEquals(original.poblacionVivaReportada(), resuelta.poblacionVivaReportada());
    assertEquals(1, resuelta.version());
    assertThrows(
        SolicitudAislamientoNoPendienteException.class,
        () -> resuelta.decidir(DecisionAislamiento.RECHAZAR, "No", "veterinario", Instant.now()));
  }

  @Test
  void rechazaTextosVaciosYDecisionIncompleta() {
    SolicitudAislamiento original = pendiente();
    assertThrows(
        SolicitudAislamientoInvalidaException.class,
        () ->
            new SolicitudAislamiento(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                " ",
                "Tos",
                8000,
                5,
                "trabajador",
                Instant.now(),
                EstadoSolicitudAislamiento.PENDIENTE_VALIDACION,
                null,
                null,
                null,
                0));
    assertThrows(
        SolicitudAislamientoInvalidaException.class,
        () -> original.decidir(DecisionAislamiento.RECHAZAR, " ", "veterinario", Instant.now()));
  }
}
