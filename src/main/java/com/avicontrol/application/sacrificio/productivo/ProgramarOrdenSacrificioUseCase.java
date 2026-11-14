package com.avicontrol.application.sacrificio.productivo;

import com.avicontrol.application.sacrificio.productivo.result.OrdenSacrificioResult;
import com.avicontrol.domain.exception.galpon.GalponNoEncontradoException;
import com.avicontrol.domain.exception.sacrificio.productivo.LoteActualNoEncontradoException;
import com.avicontrol.domain.exception.sacrificio.productivo.PrecondicionSacrificioNoCumplidaException;
import com.avicontrol.domain.model.sacrificio.productivo.OrdenSacrificioProductivo;
import com.avicontrol.domain.port.out.modulo1.GalponQueryPort;
import com.avicontrol.domain.port.out.modulo1.LoteQueryPort;
import com.avicontrol.domain.port.out.sacrificio.productivo.OrdenSacrificioProductivoRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class ProgramarOrdenSacrificioUseCase {
  private final GalponQueryPort galpones;
  private final LoteQueryPort lotes;
  private final OrdenSacrificioProductivoRepositoryPort ordenes;
  private final Clock clock;

  public ProgramarOrdenSacrificioUseCase(
      GalponQueryPort galpones,
      LoteQueryPort lotes,
      OrdenSacrificioProductivoRepositoryPort ordenes,
      Clock clock) {
    this.galpones = galpones;
    this.lotes = lotes;
    this.ordenes = ordenes;
    this.clock = clock;
  }

  @Transactional
  public OrdenSacrificioResult programar(UUID galponId, Instant fecha) {
    if (galponId == null) throw new IllegalArgumentException("galponId es obligatorio");
    var galpon = galpones.consultar(galponId).orElseThrow(GalponNoEncontradoException::new);
    var lote = lotes.consultarActual(galponId).orElseThrow(LoteActualNoEncontradoException::new);
    if (!"EN_COSECHA".equals(galpon.estado()) || !lote.galponId().equals(galponId))
      throw new PrecondicionSacrificioNoCumplidaException();
    if (ordenes.existePorGalponLote(galponId, lote.id()))
      throw new PrecondicionSacrificioNoCumplidaException();
    OrdenSacrificioProductivo orden =
        OrdenSacrificioProductivo.programar(galponId, lote.id(), fecha, Instant.now(clock));
    ordenes.crear(orden);
    return OrdenSacrificioResult.desde(orden);
  }
}
