package com.avicontrol.application.sacrificio.productivo;

import com.avicontrol.domain.exception.sacrificio.productivo.PrecondicionSacrificioNoCumplidaException;
import com.avicontrol.domain.port.out.modulo1.GalponQueryPort;
import com.avicontrol.domain.port.out.modulo1.GalponUpdatePort;
import com.avicontrol.domain.port.out.modulo1.LoteQueryPort;
import com.avicontrol.domain.port.out.modulo1.LoteUpdatePort;
import com.avicontrol.domain.port.out.sacrificio.productivo.OrdenSacrificioProductivoRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class EjecutarOrdenSacrificioUseCase {
  private final OrdenSacrificioProductivoRepositoryPort ordenes;
  private final GalponQueryPort galpones;
  private final LoteQueryPort lotes;
  private final GalponUpdatePort cambiosGalpon;
  private final LoteUpdatePort cambiosLote;
  private final Clock clock;

  public EjecutarOrdenSacrificioUseCase(
      OrdenSacrificioProductivoRepositoryPort ordenes,
      GalponQueryPort galpones,
      LoteQueryPort lotes,
      GalponUpdatePort cambiosGalpon,
      LoteUpdatePort cambiosLote,
      Clock clock) {
    this.ordenes = ordenes;
    this.galpones = galpones;
    this.lotes = lotes;
    this.cambiosGalpon = cambiosGalpon;
    this.cambiosLote = cambiosLote;
    this.clock = clock;
  }

  @Transactional
  public boolean ejecutar(UUID id) {
    var actual = ordenes.bloquear(id).orElse(null);
    if (actual == null || !actual.estado().pendiente()) return false;
    Instant ahora = Instant.now(clock);
    if (actual.fechaHoraProgramada().isAfter(ahora)) return false;
    var galpon = galpones.consultar(actual.galponId());
    var lote = lotes.consultarActual(actual.galponId());
    if (galpon.isEmpty()
        || lote.isEmpty()
        || !"EN_COSECHA".equals(galpon.get().estado())
        || !actual.loteId().equals(lote.get().id()))
      throw new PrecondicionSacrificioNoCumplidaException();
    if (!cambiosGalpon.vaciarSiEnCosecha(actual.galponId(), actual.loteId())
        || !cambiosLote.finalizarAlojamiento(actual.galponId(), actual.loteId())
        || !ordenes.actualizar(actual.ejecutar(ahora)))
      throw new PrecondicionSacrificioNoCumplidaException();
    return true;
  }
}
