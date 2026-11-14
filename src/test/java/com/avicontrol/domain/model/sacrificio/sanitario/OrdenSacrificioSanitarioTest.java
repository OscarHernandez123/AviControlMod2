package com.avicontrol.domain.model.sacrificio.sanitario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.avicontrol.domain.exception.sacrificio.sanitario.OrdenSanitariaNoPendienteException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrdenSacrificioSanitarioTest {
  @Test
  void soloEjecutaUnaVezYConPoblacionPositiva() {
    var orden =
        OrdenSacrificioSanitario.emitir(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Instant.now());
    assertEquals(EstadoOrdenSacrificioSanitario.PENDIENTE, orden.estado());
    var ejecutada = orden.ejecutar(800, Instant.now());
    assertEquals(800, ejecutada.poblacionSacrificada());
    assertThrows(
        OrdenSanitariaNoPendienteException.class, () -> ejecutada.ejecutar(800, Instant.now()));
  }
}
