package com.avicontrol.application.galpon;

import com.avicontrol.application.galpon.result.EdadLoteResult;
import com.avicontrol.domain.exception.galpon.GalponNoEncontradoException;
import com.avicontrol.domain.exception.galpon.LoteActualInconsistenteException;
import com.avicontrol.domain.port.out.modulo1.GalponQueryPort;
import com.avicontrol.domain.port.out.modulo1.LoteQueryPort;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

public class ConsultarEdadPorGalponUseCase {
  private final GalponQueryPort galpones;
  private final LoteQueryPort lotes;
  private final Clock clock;

  public ConsultarEdadPorGalponUseCase(GalponQueryPort galpones, LoteQueryPort lotes, Clock clock) {
    this.galpones = galpones;
    this.lotes = lotes;
    this.clock = clock;
  }

  public EdadLoteResult consultar(UUID galponId) {
    if (galponId == null) throw new IllegalArgumentException("galponId es obligatorio");
    galpones.consultar(galponId).orElseThrow(GalponNoEncontradoException::new);
    var lote = lotes.consultarActual(galponId);
    if (lote.isEmpty())
      return new EdadLoteResult(
          galponId, null, null, "El galpón no tiene lote alojado actualmente");
    if (!lote.get().galponId().equals(galponId)) throw new LoteActualInconsistenteException();
    return new EdadLoteResult(
        galponId, lote.get().id(), lote.get().calcularEdad(LocalDate.now(clock)), null);
  }
}
