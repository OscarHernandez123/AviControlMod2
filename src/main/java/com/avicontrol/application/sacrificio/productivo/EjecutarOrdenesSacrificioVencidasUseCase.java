package com.avicontrol.application.sacrificio.productivo;

import com.avicontrol.domain.port.out.sacrificio.productivo.OrdenSacrificioProductivoRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EjecutarOrdenesSacrificioVencidasUseCase {
  private static final Logger log =
      LoggerFactory.getLogger(EjecutarOrdenesSacrificioVencidasUseCase.class);
  private final OrdenSacrificioProductivoRepositoryPort ordenes;
  private final EjecutarOrdenSacrificioUseCase ejecucion;
  private final Clock clock;

  public EjecutarOrdenesSacrificioVencidasUseCase(
      OrdenSacrificioProductivoRepositoryPort ordenes,
      EjecutarOrdenSacrificioUseCase ejecucion,
      Clock clock) {
    this.ordenes = ordenes;
    this.ejecucion = ejecucion;
    this.clock = clock;
  }

  public int ejecutar() {
    int total = 0;
    for (var id : ordenes.vencidas(Instant.now(clock))) {
      try {
        if (ejecucion.ejecutar(id)) total++;
      } catch (RuntimeException exception) {
        log.warn("Fallo al ejecutar orden de sacrificio {}. Se reintentará", id, exception);
      }
    }
    return total;
  }
}
