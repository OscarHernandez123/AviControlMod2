package com.avicontrol.application.sacrificio.productivo;

import com.avicontrol.application.sacrificio.productivo.result.OrdenSacrificioResult;
import com.avicontrol.domain.exception.sacrificio.productivo.PrecondicionSacrificioNoCumplidaException;
import com.avicontrol.domain.port.out.modulo1.GalponQueryPort;
import com.avicontrol.domain.port.out.modulo1.LoteQueryPort;
import com.avicontrol.domain.port.out.sacrificio.productivo.OrdenSacrificioProductivoRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class ReprogramarOrdenSacrificioUseCase {
  private final OrdenSacrificioProductivoRepositoryPort ordenes;
  private final GalponQueryPort galpones;
  private final LoteQueryPort lotes;
  private final Clock clock;

  public ReprogramarOrdenSacrificioUseCase(
      OrdenSacrificioProductivoRepositoryPort ordenes,
      GalponQueryPort galpones,
      LoteQueryPort lotes,
      Clock clock) {
    this.ordenes = ordenes;
    this.galpones = galpones;
    this.lotes = lotes;
    this.clock = clock;
  }

  @Transactional
  public OrdenSacrificioResult reprogramar(UUID id, Instant fecha) {
    var actual =
        ordenes.bloquear(id).orElseThrow(() -> new NoSuchElementException("Orden no encontrada"));
    var galpon =
        galpones
            .consultar(actual.galponId())
            .orElseThrow(PrecondicionSacrificioNoCumplidaException::new);
    var lote = lotes.consultarActual(actual.galponId());
    if (!"EN_COSECHA".equals(galpon.estado())
        || lote.isEmpty()
        || !lote.get().id().equals(actual.loteId()))
      throw new PrecondicionSacrificioNoCumplidaException();
    var nueva = actual.reprogramar(fecha, Instant.now(clock));
    if (!ordenes.actualizar(nueva)) throw new PrecondicionSacrificioNoCumplidaException();
    return OrdenSacrificioResult.desde(nueva);
  }
}
