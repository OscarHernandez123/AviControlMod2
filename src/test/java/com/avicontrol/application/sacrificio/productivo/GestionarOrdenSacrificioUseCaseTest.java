package com.avicontrol.application.sacrificio.productivo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.avicontrol.domain.exception.sacrificio.productivo.PrecondicionSacrificioNoCumplidaException;
import com.avicontrol.domain.model.lote.Lote;
import com.avicontrol.domain.port.out.modulo1.GalponQueryPort;
import com.avicontrol.domain.port.out.modulo1.LoteQueryPort;
import com.avicontrol.domain.port.out.sacrificio.productivo.OrdenSacrificioProductivoRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GestionarOrdenSacrificioUseCaseTest {
  @Test
  void noProgramaSiGalponNoEstaEnCosecha() {
    var galpones = mock(GalponQueryPort.class);
    var lotes = mock(LoteQueryPort.class);
    var ordenes = mock(OrdenSacrificioProductivoRepositoryPort.class);
    UUID id = UUID.randomUUID();
    when(galpones.consultar(id))
        .thenReturn(Optional.of(new GalponQueryPort.GalponDatos(id, "G", 9000, "PRODUCTIVO")));
    when(lotes.consultarActual(id))
        .thenReturn(Optional.of(new Lote(UUID.randomUUID(), id, 8000, LocalDate.now())));
    var caso = new ProgramarOrdenSacrificioUseCase(galpones, lotes, ordenes, Clock.systemUTC());
    assertThrows(
        PrecondicionSacrificioNoCumplidaException.class,
        () -> caso.programar(id, Instant.now().plusSeconds(3600)));
    verifyNoInteractions(ordenes);
  }

  @Test
  void cancelacionDeOrdenAusenteDa404DeDominio() {
    var ordenes = mock(OrdenSacrificioProductivoRepositoryPort.class);
    UUID id = UUID.randomUUID();
    assertEquals(
        "Orden no encontrada",
        assertThrows(
                java.util.NoSuchElementException.class,
                () -> new CancelarOrdenSacrificioUseCase(ordenes, Clock.systemUTC()).cancelar(id))
            .getMessage());
  }
}
