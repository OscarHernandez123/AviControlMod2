package com.avicontrol.application.sanitario.diagnostico;

import com.avicontrol.domain.exception.sanitario.aislamiento.Modulo1NoDisponibleException;
import com.avicontrol.domain.model.sanitario.aislamiento.GalponLoteAislamiento;
import com.avicontrol.domain.model.sanitario.diagnostico.DiagnosticoGalpon;
import com.avicontrol.domain.model.sanitario.diagnostico.EstadoDiagnostico;
import com.avicontrol.domain.port.out.sanitario.aislamiento.GalponLoteAislamientoQueryPort;
import com.avicontrol.domain.port.out.sanitario.diagnostico.DiagnosticoGalponRepositoryPort;
import com.avicontrol.domain.port.out.sanitario.diagnostico.GalponReintegroPort;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;

public class ReintegrarGalponesTratadosUseCase {
  private static final Logger log =
      LoggerFactory.getLogger(ReintegrarGalponesTratadosUseCase.class);
  private final DiagnosticoGalponRepositoryPort diagnosticos;
  private final GalponLoteAislamientoQueryPort galpones;
  private final GalponReintegroPort reintegro;
  private final Clock clock;
  private final TransactionTemplate transaction;

  public ReintegrarGalponesTratadosUseCase(
      DiagnosticoGalponRepositoryPort diagnosticos,
      GalponLoteAislamientoQueryPort galpones,
      GalponReintegroPort reintegro,
      Clock clock,
      TransactionTemplate transaction) {
    this.diagnosticos = diagnosticos;
    this.galpones = galpones;
    this.reintegro = reintegro;
    this.clock = clock;
    this.transaction = transaction;
  }

  public int ejecutar() {
    LocalDate hoy = LocalDate.now(clock);
    int completados = 0;
    for (UUID id : diagnosticos.vencidos(hoy)) {
      try {
        Boolean cambiado = transaction.execute(status -> procesar(id, hoy));
        if (Boolean.TRUE.equals(cambiado)) completados++;
      } catch (RuntimeException e) {
        log.warn("Reintegro pendiente para diagnóstico {}: {}", id, e.getMessage());
      }
    }
    return completados;
  }

  private boolean procesar(UUID id, LocalDate hoy) {
    DiagnosticoGalpon d = diagnosticos.bloquear(id).orElse(null);
    if (d == null
        || d.estado() != EstadoDiagnostico.ACTIVO
        || d.fechaReintegro() == null
        || d.fechaReintegro().isAfter(hoy)) return false;
    GalponLoteAislamiento contexto =
        galpones.consultar(d.galponId()).orElseThrow(Modulo1NoDisponibleException::new);
    if (!d.loteId().equals(contexto.loteId()) || !"AISLAMIENTO".equals(contexto.estado())) {
      if (!diagnosticos.resolver(d, d.resolver(EstadoDiagnostico.REINTEGRO_OMITIDO, hoy)))
        throw new IllegalStateException("El diagnóstico cambió durante el reintegro");
      return false;
    }
    if (!reintegro.reintegrarSiAislado(d.galponId(), d.loteId()))
      throw new IllegalStateException("El galpón cambió durante el reintegro");
    if (!diagnosticos.resolver(d, d.resolver(EstadoDiagnostico.REINTEGRADO, hoy)))
      throw new IllegalStateException("El diagnóstico cambió durante el reintegro");
    return true;
  }
}
