package com.avicontrol.application.sacrificio.productivo;

import com.avicontrol.domain.exception.sacrificio.productivo.PrecondicionSacrificioNoCumplidaException;
import com.avicontrol.domain.port.out.sacrificio.productivo.ResultadoSacrificioRepositoryPort;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class MarcarResultadoUtilizadoUseCase {
  private final ResultadoSacrificioRepositoryPort resultados;

  public MarcarResultadoUtilizadoUseCase(ResultadoSacrificioRepositoryPort resultados) {
    this.resultados = resultados;
  }

  @Transactional
  public void confirmar(UUID eventoId, UUID resultadoId, UUID loteId) {
    if (eventoId == null || resultadoId == null || loteId == null)
      throw new IllegalArgumentException("Evento de utilización incompleto");
    var resultado =
        resultados
            .bloquear(resultadoId)
            .orElseThrow(() -> new NoSuchElementException("Resultado no encontrado"));
    if (!resultado.loteId().equals(loteId)) throw new PrecondicionSacrificioNoCumplidaException();
    if (resultados.registrarConfirmacion(eventoId, resultadoId))
      resultados.marcarUtilizado(resultadoId);
  }
}
