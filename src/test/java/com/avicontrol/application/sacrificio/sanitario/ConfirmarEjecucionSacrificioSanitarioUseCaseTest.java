package com.avicontrol.application.sacrificio.sanitario;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.avicontrol.domain.exception.sacrificio.sanitario.OrdenSanitariaNoPendienteException;
import com.avicontrol.domain.model.sacrificio.sanitario.OrdenSacrificioSanitario;
import com.avicontrol.domain.port.out.modulo1.GalponUpdatePort;
import com.avicontrol.domain.port.out.modulo1.LoteQueryPort;
import com.avicontrol.domain.port.out.modulo1.LoteUpdatePort;
import com.avicontrol.domain.port.out.sacrificio.sanitario.OrdenSacrificioSanitarioRepositoryPort;
import com.avicontrol.domain.port.out.sanitario.aislamiento.GalponLoteAislamientoQueryPort;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConfirmarEjecucionSacrificioSanitarioUseCaseTest {
  @Test
  void ordenEjecutadaNoTocaModuloUno() {
    var ordenes = mock(OrdenSacrificioSanitarioRepositoryPort.class);
    var galpones = mock(GalponLoteAislamientoQueryPort.class);
    var lotes = mock(LoteQueryPort.class);
    var cambiosGalpon = mock(GalponUpdatePort.class);
    var cambiosLote = mock(LoteUpdatePort.class);
    var orden =
        OrdenSacrificioSanitario.emitir(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Instant.now())
            .ejecutar(100, Instant.now());
    when(ordenes.bloquear(orden.id())).thenReturn(Optional.of(orden));
    var caso =
        new ConfirmarEjecucionSacrificioSanitarioUseCase(
            ordenes, galpones, lotes, cambiosGalpon, cambiosLote, Clock.systemUTC());
    assertThrows(OrdenSanitariaNoPendienteException.class, () -> caso.confirmar(orden.id()));
    verifyNoInteractions(galpones, lotes, cambiosGalpon, cambiosLote);
  }
}
