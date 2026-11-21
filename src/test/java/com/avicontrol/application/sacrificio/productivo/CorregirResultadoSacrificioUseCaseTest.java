package com.avicontrol.application.sacrificio.productivo;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.avicontrol.domain.exception.sacrificio.productivo.ResultadoSacrificioNoModificableException;
import com.avicontrol.domain.model.sacrificio.productivo.EstadoOrdenSacrificio;
import com.avicontrol.domain.model.sacrificio.productivo.EstadoResultadoSacrificio;
import com.avicontrol.domain.model.sacrificio.productivo.OrdenSacrificioProductivo;
import com.avicontrol.domain.model.sacrificio.productivo.ResultadoSacrificioProductivo;
import com.avicontrol.domain.port.out.sacrificio.productivo.OrdenSacrificioProductivoQueryPort;
import com.avicontrol.domain.port.out.sacrificio.productivo.ResultadoSacrificioRepositoryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CorregirResultadoSacrificioUseCaseTest {
  @Test
  void noCorrigeResultadoUtilizado() {
    var ordenes = mock(OrdenSacrificioProductivoQueryPort.class);
    var resultados = mock(ResultadoSacrificioRepositoryPort.class);
    UUID ordenId = UUID.randomUUID();
    UUID galponId = UUID.randomUUID();
    UUID loteId = UUID.randomUUID();
    Instant ahora = Instant.now();
    when(ordenes.buscar(ordenId))
        .thenReturn(
            Optional.of(
                new OrdenSacrificioProductivo(
                    ordenId,
                    galponId,
                    loteId,
                    EstadoOrdenSacrificio.EJECUTADA,
                    ahora,
                    ahora,
                    null,
                    null,
                    ahora,
                    1)));
    when(resultados.buscarPorOrden(ordenId))
        .thenReturn(
            Optional.of(
                new ResultadoSacrificioProductivo(
                    UUID.randomUUID(),
                    ordenId,
                    galponId,
                    loteId,
                    7800,
                    new BigDecimal("18720.50"),
                    EstadoResultadoSacrificio.UTILIZADO,
                    ahora,
                    null,
                    1)));
    assertThrows(
        ResultadoSacrificioNoModificableException.class,
        () ->
            new CorregirResultadoSacrificioUseCase(ordenes, resultados, Clock.systemUTC())
                .corregir(ordenId, 7790, new BigDecimal("18700.00")));
  }
}
