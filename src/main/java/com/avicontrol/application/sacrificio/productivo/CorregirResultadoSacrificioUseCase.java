package com.avicontrol.application.sacrificio.productivo;

import com.avicontrol.application.sacrificio.productivo.result.ResultadoSacrificioResult;
import com.avicontrol.domain.exception.sacrificio.productivo.OrdenNoModificableException;
import com.avicontrol.domain.exception.sacrificio.productivo.ResultadoSacrificioNoModificableException;
import com.avicontrol.domain.model.sacrificio.productivo.EstadoOrdenSacrificio;
import com.avicontrol.domain.port.out.sacrificio.productivo.OrdenSacrificioProductivoQueryPort;
import com.avicontrol.domain.port.out.sacrificio.productivo.ResultadoSacrificioRepositoryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class CorregirResultadoSacrificioUseCase {
  private final OrdenSacrificioProductivoQueryPort ordenes;
  private final ResultadoSacrificioRepositoryPort resultados;
  private final Clock clock;

  public CorregirResultadoSacrificioUseCase(
      OrdenSacrificioProductivoQueryPort ordenes,
      ResultadoSacrificioRepositoryPort resultados,
      Clock clock) {
    this.ordenes = ordenes;
    this.resultados = resultados;
    this.clock = clock;
  }

  @Transactional
  public ResultadoSacrificioResult corregir(UUID ordenId, int cantidad, BigDecimal peso) {
    var orden =
        ordenes
            .buscar(ordenId)
            .orElseThrow(() -> new NoSuchElementException("Orden no encontrada"));
    if (orden.estado() != EstadoOrdenSacrificio.EJECUTADA) throw new OrdenNoModificableException();
    var actual =
        resultados
            .buscarPorOrden(ordenId)
            .orElseThrow(() -> new NoSuchElementException("Resultado no encontrado"));
    var nuevo = actual.corregir(cantidad, peso, Instant.now(clock));
    if (!resultados.actualizarSiDisponible(nuevo))
      throw new ResultadoSacrificioNoModificableException();
    return ResultadoSacrificioResult.desde(nuevo);
  }
}
