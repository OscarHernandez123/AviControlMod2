package com.avicontrol.application.sacrificio.productivo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.avicontrol.domain.port.out.sacrificio.productivo.OrdenSacrificioProductivoRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EjecutarOrdenesSacrificioVencidasUseCaseTest {
  @Test
  void falloDeUnaOrdenNoBloqueaLasDemas() {
    var ordenes = mock(OrdenSacrificioProductivoRepositoryPort.class);
    var ejecucion = mock(EjecutarOrdenSacrificioUseCase.class);
    UUID fallida = UUID.randomUUID();
    UUID siguiente = UUID.randomUUID();
    Instant ahora = Instant.parse("2026-11-07T12:00:00Z");
    when(ordenes.vencidas(ahora)).thenReturn(List.of(fallida, siguiente));
    when(ejecucion.ejecutar(fallida))
        .thenThrow(new IllegalStateException("Módulo 1 no disponible"));
    when(ejecucion.ejecutar(siguiente)).thenReturn(true);
    assertEquals(
        1,
        new EjecutarOrdenesSacrificioVencidasUseCase(
                ordenes, ejecucion, Clock.fixed(ahora, ZoneOffset.UTC))
            .ejecutar());
  }
}
