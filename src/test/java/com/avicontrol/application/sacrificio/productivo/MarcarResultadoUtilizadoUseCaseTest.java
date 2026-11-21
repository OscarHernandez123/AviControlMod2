package com.avicontrol.application.sacrificio.productivo;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.avicontrol.domain.exception.sacrificio.productivo.PrecondicionSacrificioNoCumplidaException;
import com.avicontrol.domain.model.sacrificio.productivo.EstadoResultadoSacrificio;
import com.avicontrol.domain.model.sacrificio.productivo.ResultadoSacrificioProductivo;
import com.avicontrol.domain.port.out.sacrificio.productivo.ResultadoSacrificioRepositoryPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MarcarResultadoUtilizadoUseCaseTest {
  @Mock ResultadoSacrificioRepositoryPort resultados;
  @InjectMocks MarcarResultadoUtilizadoUseCase useCase;

  @Test
  void marcaResultadoUtilizadoConIdempotenciaPorEvento() {
    UUID eventoId = UUID.randomUUID();
    UUID resultadoId = UUID.randomUUID();
    UUID loteId = UUID.randomUUID();
    ResultadoSacrificioProductivo res =
        new ResultadoSacrificioProductivo(
            resultadoId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            loteId,
            900,
            new BigDecimal("2100"),
            EstadoResultadoSacrificio.DISPONIBLE,
            Instant.now(),
            null,
            0L);
    when(resultados.bloquear(resultadoId)).thenReturn(Optional.of(res));
    when(resultados.registrarConfirmacion(eventoId, resultadoId)).thenReturn(true, false);

    useCase.confirmar(eventoId, resultadoId, loteId);
    verify(resultados).marcarUtilizado(resultadoId);

    useCase.confirmar(eventoId, resultadoId, loteId);
    verify(resultados, org.mockito.Mockito.times(1)).marcarUtilizado(resultadoId);
  }

  @Test
  void rechazaLoteDistintoOParametrosNulosOResultadoAusente() {
    assertThatThrownBy(() -> useCase.confirmar(null, UUID.randomUUID(), UUID.randomUUID()))
        .isInstanceOf(IllegalArgumentException.class);

    UUID resultadoId = UUID.randomUUID();
    when(resultados.bloquear(resultadoId)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> useCase.confirmar(UUID.randomUUID(), resultadoId, UUID.randomUUID()))
        .isInstanceOf(NoSuchElementException.class);

    ResultadoSacrificioProductivo res =
        new ResultadoSacrificioProductivo(
            resultadoId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            900,
            new BigDecimal("2100"),
            EstadoResultadoSacrificio.DISPONIBLE,
            Instant.now(),
            null,
            0L);
    when(resultados.bloquear(resultadoId)).thenReturn(Optional.of(res));
    assertThatThrownBy(() -> useCase.confirmar(UUID.randomUUID(), resultadoId, UUID.randomUUID()))
        .isInstanceOf(PrecondicionSacrificioNoCumplidaException.class);
    verify(resultados, never()).marcarUtilizado(resultadoId);
  }
}
