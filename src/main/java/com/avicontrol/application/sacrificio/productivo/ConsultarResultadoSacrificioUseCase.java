package com.avicontrol.application.sacrificio.productivo;

import com.avicontrol.application.sacrificio.productivo.result.ResultadoSacrificioResult;
import com.avicontrol.domain.port.out.sacrificio.productivo.ResultadoSacrificioRepositoryPort;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class ConsultarResultadoSacrificioUseCase {
  private final ResultadoSacrificioRepositoryPort resultados;

  public ConsultarResultadoSacrificioUseCase(ResultadoSacrificioRepositoryPort resultados) {
    this.resultados = resultados;
  }

  @Transactional(readOnly = true)
  public ResultadoSacrificioResult consultarPorLote(UUID loteId) {
    if (loteId == null) throw new IllegalArgumentException("loteId es obligatorio");
    return ResultadoSacrificioResult.desde(
        resultados
            .buscarPorLote(loteId)
            .orElseThrow(() -> new NoSuchElementException("Resultado no encontrado para el lote")));
  }

  @Transactional(readOnly = true)
  public ResultadoSacrificioResult consultar(UUID ordenId) {
    return ResultadoSacrificioResult.desde(
        resultados
            .buscarPorOrden(ordenId)
            .orElseThrow(() -> new NoSuchElementException("Resultado no encontrado")));
  }
}
