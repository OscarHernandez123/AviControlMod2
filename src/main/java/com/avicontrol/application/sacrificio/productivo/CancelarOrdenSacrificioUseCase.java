package com.avicontrol.application.sacrificio.productivo;

import com.avicontrol.application.sacrificio.productivo.result.OrdenSacrificioResult;
import com.avicontrol.domain.exception.sacrificio.productivo.PrecondicionSacrificioNoCumplidaException;
import com.avicontrol.domain.port.out.sacrificio.productivo.OrdenSacrificioProductivoRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class CancelarOrdenSacrificioUseCase {
  private final OrdenSacrificioProductivoRepositoryPort ordenes;
  private final Clock clock;

  public CancelarOrdenSacrificioUseCase(
      OrdenSacrificioProductivoRepositoryPort ordenes, Clock clock) {
    this.ordenes = ordenes;
    this.clock = clock;
  }

  @Transactional
  public OrdenSacrificioResult cancelar(UUID id) {
    var actual =
        ordenes.bloquear(id).orElseThrow(() -> new NoSuchElementException("Orden no encontrada"));
    var nueva = actual.cancelar(Instant.now(clock));
    if (!ordenes.actualizar(nueva)) throw new PrecondicionSacrificioNoCumplidaException();
    return OrdenSacrificioResult.desde(nueva);
  }
}
