package com.avicontrol.application.sacrificio.sanitario;

import com.avicontrol.application.sacrificio.sanitario.result.OrdenSacrificioSanitarioResult;
import com.avicontrol.domain.exception.sacrificio.sanitario.DiagnosticoNoElegibleException;
import com.avicontrol.domain.exception.sacrificio.sanitario.PoblacionNoSacrificableException;
import com.avicontrol.domain.model.sacrificio.sanitario.OrdenSacrificioSanitario;
import com.avicontrol.domain.port.out.modulo1.LoteQueryPort;
import com.avicontrol.domain.port.out.sacrificio.sanitario.DiagnosticoSanitarioQueryPort;
import com.avicontrol.domain.port.out.sacrificio.sanitario.OrdenSacrificioSanitarioRepositoryPort;
import com.avicontrol.domain.port.out.sanitario.aislamiento.GalponLoteAislamientoQueryPort;
import java.time.Clock;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

public class EmitirOrdenSacrificioSanitarioUseCase {
  private final DiagnosticoSanitarioQueryPort diagnosticos;
  private final GalponLoteAislamientoQueryPort galpones;
  private final LoteQueryPort lotes;
  private final OrdenSacrificioSanitarioRepositoryPort ordenes;
  private final Clock clock;

  public EmitirOrdenSacrificioSanitarioUseCase(
      DiagnosticoSanitarioQueryPort diagnosticos,
      GalponLoteAislamientoQueryPort galpones,
      LoteQueryPort lotes,
      OrdenSacrificioSanitarioRepositoryPort ordenes,
      Clock clock) {
    this.diagnosticos = diagnosticos;
    this.galpones = galpones;
    this.lotes = lotes;
    this.ordenes = ordenes;
    this.clock = clock;
  }

  @Transactional(isolation = Isolation.SERIALIZABLE)
  public OrdenSacrificioSanitarioResult emitir(UUID galponId) {
    var diagnostico =
        diagnosticos
            .activoPorGalpon(galponId)
            .orElseThrow(() -> new NoSuchElementException("Diagnóstico activo no encontrado"));
    if (!diagnostico.requiereSacrificioSanitario()) throw new DiagnosticoNoElegibleException();
    var galpon =
        galpones
            .consultar(galponId)
            .orElseThrow(() -> new NoSuchElementException("Galpón no encontrado"));
    if (!"AISLAMIENTO".equals(galpon.estado())) throw new DiagnosticoNoElegibleException();
    var lote = lotes.consultarActual(galponId).orElseThrow(PoblacionNoSacrificableException::new);
    if (!diagnostico.loteId().equals(lote.id()) || !lote.id().equals(galpon.loteId()))
      throw new DiagnosticoNoElegibleException();
    if (lote.poblacionActual() <= 0) throw new PoblacionNoSacrificableException();
    var orden =
        OrdenSacrificioSanitario.emitir(diagnostico.id(), galponId, lote.id(), Instant.now(clock));
    ordenes.crear(orden);
    return OrdenSacrificioSanitarioResult.de(orden);
  }
}
