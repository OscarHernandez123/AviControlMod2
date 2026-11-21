package com.avicontrol.application.sacrificio.productivo;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.avicontrol.domain.exception.sacrificio.productivo.OrdenNoModificableException;
import com.avicontrol.domain.model.sacrificio.productivo.EstadoOrdenSacrificio;
import com.avicontrol.domain.model.sacrificio.productivo.OrdenSacrificioProductivo;
import com.avicontrol.domain.port.out.sacrificio.productivo.OrdenSacrificioProductivoQueryPort;
import com.avicontrol.domain.port.out.sacrificio.productivo.ResultadoSacrificioRepositoryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RegistrarResultadoSacrificioUseCaseTest {
  @Test
  void rechazaOrdenNoEjecutadaAntesDeEscribir() {
    var ordenes = mock(OrdenSacrificioProductivoQueryPort.class);
    var resultados = mock(ResultadoSacrificioRepositoryPort.class);
    UUID id = UUID.randomUUID();
    when(ordenes.buscar(id))
        .thenReturn(
            Optional.of(
                new OrdenSacrificioProductivo(
                    id,
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    EstadoOrdenSacrificio.PROGRAMADA,
                    Instant.now(),
                    Instant.now(),
                    null,
                    null,
                    null,
                    0)));
    var caso = new RegistrarResultadoSacrificioUseCase(ordenes, resultados, Clock.systemUTC());
    assertThrows(
        OrdenNoModificableException.class,
        () -> caso.registrar(id, 7800, new BigDecimal("18720.50")));
    verifyNoInteractions(resultados);
  }
}
