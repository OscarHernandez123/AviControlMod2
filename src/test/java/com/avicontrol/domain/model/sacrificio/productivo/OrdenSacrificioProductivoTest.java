package com.avicontrol.domain.model.sacrificio.productivo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.avicontrol.domain.exception.sacrificio.productivo.FechaProgramadaInvalidaException;
import com.avicontrol.domain.exception.sacrificio.productivo.OrdenNoModificableException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrdenSacrificioProductivoTest {
  private static final Instant AHORA = Instant.parse("2026-11-07T12:00:00Z");

  @Test
  void transicionesYFechas() {
    var orden =
        OrdenSacrificioProductivo.programar(
            UUID.randomUUID(), UUID.randomUUID(), AHORA.plusSeconds(3600), AHORA);
    assertEquals(EstadoOrdenSacrificio.PROGRAMADA, orden.estado());
    var reprogramada = orden.reprogramar(AHORA.plusSeconds(7200), AHORA.plusSeconds(1));
    assertEquals(EstadoOrdenSacrificio.REPROGRAMADA, reprogramada.estado());
    assertEquals(1, reprogramada.version());
    var cancelada = reprogramada.cancelar(AHORA.plusSeconds(2));
    assertEquals(EstadoOrdenSacrificio.CANCELADA, cancelada.estado());
    assertThrows(
        OrdenNoModificableException.class,
        () -> cancelada.reprogramar(AHORA.plusSeconds(9000), AHORA));
    var ejecutada = orden.ejecutar(AHORA.plusSeconds(3600));
    assertEquals(EstadoOrdenSacrificio.EJECUTADA, ejecutada.estado());
    assertThrows(
        OrdenNoModificableException.class, () -> ejecutada.cancelar(AHORA.plusSeconds(4000)));
    assertThrows(OrdenNoModificableException.class, () -> orden.cancelar(AHORA.plusSeconds(3600)));
    assertThrows(
        OrdenNoModificableException.class,
        () -> orden.reprogramar(AHORA.plusSeconds(7200), AHORA.plusSeconds(3600)));
    assertThrows(
        FechaProgramadaInvalidaException.class,
        () ->
            OrdenSacrificioProductivo.programar(
                UUID.randomUUID(), UUID.randomUUID(), AHORA, AHORA));
  }
}
