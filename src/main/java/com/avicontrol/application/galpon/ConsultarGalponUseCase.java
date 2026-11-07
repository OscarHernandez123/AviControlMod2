package com.avicontrol.application.galpon;

import com.avicontrol.application.galpon.result.GalponDetalleResult;
import com.avicontrol.domain.exception.galpon.FechaIngresoInconsistenteException;
import com.avicontrol.domain.exception.galpon.GalponNoEncontradoException;
import com.avicontrol.domain.exception.galpon.LoteActualInconsistenteException;
import com.avicontrol.domain.model.lote.Edad;
import com.avicontrol.domain.port.out.modulo1.GalponQueryPort;
import com.avicontrol.domain.port.out.modulo1.LoteQueryPort;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

public class ConsultarGalponUseCase {
  private final GalponQueryPort galpones;
  private final LoteQueryPort lotes;
  private final Clock clock;

  public ConsultarGalponUseCase(GalponQueryPort galpones, LoteQueryPort lotes, Clock clock) {
    this.galpones = galpones;
    this.lotes = lotes;
    this.clock = clock;
  }

  public GalponDetalleResult consultar(UUID galponId) {
    if (galponId == null) throw new IllegalArgumentException("galponId es obligatorio");
    var galpon = galpones.consultar(galponId).orElseThrow(GalponNoEncontradoException::new);
    var lote = lotes.consultarActual(galponId);
    if (lote.isEmpty())
      return new GalponDetalleResult(
          galpon.id(),
          galpon.nombre(),
          galpon.aforoMaximo(),
          galpon.estado(),
          null,
          "El galpón no tiene lote alojado actualmente");
    if (!lote.get().galponId().equals(galponId)) throw new LoteActualInconsistenteException();
    Edad edad;
    String mensaje = null;
    try {
      edad = lote.get().calcularEdad(LocalDate.now(clock));
    } catch (FechaIngresoInconsistenteException exception) {
      edad = null;
      mensaje = exception.getMessage();
    }
    return new GalponDetalleResult(
        galpon.id(),
        galpon.nombre(),
        galpon.aforoMaximo(),
        galpon.estado(),
        new GalponDetalleResult.LoteActual(
            lote.get().id(), lote.get().poblacionActual(), edad, mensaje),
        null);
  }
}
