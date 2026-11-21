package com.avicontrol.application.sacrificio.sanitario;

import com.avicontrol.application.sacrificio.sanitario.result.OrdenSacrificioSanitarioResult;
import com.avicontrol.domain.exception.sacrificio.sanitario.DiagnosticoNoElegibleException;
import com.avicontrol.domain.exception.sacrificio.sanitario.OrdenSanitariaNoPendienteException;
import com.avicontrol.domain.exception.sacrificio.sanitario.PoblacionNoSacrificableException;
import com.avicontrol.domain.model.sacrificio.sanitario.EstadoOrdenSacrificioSanitario;
import com.avicontrol.domain.port.out.modulo1.GalponUpdatePort;
import com.avicontrol.domain.port.out.modulo1.LoteQueryPort;
import com.avicontrol.domain.port.out.modulo1.LoteUpdatePort;
import com.avicontrol.domain.port.out.sacrificio.sanitario.OrdenSacrificioSanitarioRepositoryPort;
import com.avicontrol.domain.port.out.sanitario.aislamiento.GalponLoteAislamientoQueryPort;
import java.time.Clock;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class ConfirmarEjecucionSacrificioSanitarioUseCase {
  private final OrdenSacrificioSanitarioRepositoryPort ordenes;
  private final GalponLoteAislamientoQueryPort galpones;
  private final LoteQueryPort lotes;
  private final GalponUpdatePort cambioGalpon;
  private final LoteUpdatePort cambioLote;
  private final Clock clock;

  public ConfirmarEjecucionSacrificioSanitarioUseCase(
      OrdenSacrificioSanitarioRepositoryPort ordenes,
      GalponLoteAislamientoQueryPort galpones,
      LoteQueryPort lotes,
      GalponUpdatePort cambioGalpon,
      LoteUpdatePort cambioLote,
      Clock clock) {
    this.ordenes = ordenes;
    this.galpones = galpones;
    this.lotes = lotes;
    this.cambioGalpon = cambioGalpon;
    this.cambioLote = cambioLote;
    this.clock = clock;
  }

  @Transactional
  public OrdenSacrificioSanitarioResult confirmar(UUID ordenId) {
    var orden =
        ordenes
            .bloquear(ordenId)
            .orElseThrow(() -> new NoSuchElementException("Orden sanitaria no encontrada"));
    if (orden.estado() != EstadoOrdenSacrificioSanitario.PENDIENTE)
      throw new OrdenSanitariaNoPendienteException();
    var galpon =
        galpones.consultar(orden.galponId()).orElseThrow(DiagnosticoNoElegibleException::new);
    var lote =
        lotes.consultarActual(orden.galponId()).orElseThrow(PoblacionNoSacrificableException::new);
    if (!"AISLAMIENTO".equals(galpon.estado())
        || !orden.loteId().equals(galpon.loteId())
        || !orden.loteId().equals(lote.id())) throw new DiagnosticoNoElegibleException();
    var ejecutada = orden.ejecutar(lote.poblacionActual(), Instant.now(clock));
    if (!cambioLote.sacrificarPoblacionSiPositiva(orden.galponId(), orden.loteId())
        || !cambioGalpon.vaciarSiAislado(orden.galponId(), orden.loteId())
        || !ordenes.ejecutar(ejecutada)) throw new OrdenSanitariaNoPendienteException();
    return OrdenSacrificioSanitarioResult.de(ejecutada);
  }
}
