package com.avicontrol.application.sacrificio.productivo;

import com.avicontrol.application.sacrificio.productivo.result.ResultadoSacrificioResult;
import com.avicontrol.domain.exception.sacrificio.productivo.OrdenNoModificableException;
import com.avicontrol.domain.exception.sacrificio.productivo.ResultadoSacrificioDuplicadoException;
import com.avicontrol.domain.model.sacrificio.productivo.EstadoOrdenSacrificio;
import com.avicontrol.domain.model.sacrificio.productivo.ResultadoSacrificioProductivo;
import com.avicontrol.domain.port.out.sacrificio.productivo.OrdenSacrificioProductivoQueryPort;
import com.avicontrol.domain.port.out.sacrificio.productivo.ResultadoSacrificioRepositoryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class RegistrarResultadoSacrificioUseCase {
  private final OrdenSacrificioProductivoQueryPort ordenes;
  private final ResultadoSacrificioRepositoryPort resultados;
  private final Clock clock;

  public RegistrarResultadoSacrificioUseCase(
      OrdenSacrificioProductivoQueryPort ordenes,
      ResultadoSacrificioRepositoryPort resultados,
      Clock clock) {
    this.ordenes = ordenes;
    this.resultados = resultados;
    this.clock = clock;
  }

  @Transactional
  public ResultadoSacrificioResult registrar(UUID ordenId, int cantidad, BigDecimal peso) {
    var orden =
        ordenes
            .buscar(ordenId)
            .orElseThrow(() -> new NoSuchElementException("Orden no encontrada"));
    if (orden.estado() != EstadoOrdenSacrificio.EJECUTADA) throw new OrdenNoModificableException();
    if (resultados.existePorLote(orden.loteId())) throw new ResultadoSacrificioDuplicadoException();
    var resultado =
        ResultadoSacrificioProductivo.registrar(orden, cantidad, peso, Instant.now(clock));
    resultados.crear(resultado);
    return ResultadoSacrificioResult.desde(resultado);
  }
}
