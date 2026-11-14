package com.avicontrol.application.alerta;

import com.avicontrol.domain.model.alerta.AlertaSanitaria;
import com.avicontrol.domain.model.mortalidad.RegistroMortalidadAnormal;
import com.avicontrol.domain.port.out.alerta.AlertaSanitariaRepositoryPort;
import com.avicontrol.domain.port.out.mortalidad.GenerarAlertaSanitariaPort;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class GenerarAlertaSanitariaUseCase implements GenerarAlertaSanitariaPort {
  private final AlertaSanitariaRepositoryPort alertas;
  private final Clock clock;
  private final MeterRegistry metrics;

  public GenerarAlertaSanitariaUseCase(
      AlertaSanitariaRepositoryPort alertas, Clock clock, MeterRegistry metrics) {
    this.alertas = alertas;
    this.clock = clock;
    this.metrics = metrics;
  }

  @Override
  @Transactional
  public UUID generar(RegistroMortalidadAnormal reporte) {
    if (reporte == null) throw new IllegalArgumentException("Reporte obligatorio");
    return alertas
        .porReporte(reporte.id())
        .orElseGet(
            () -> {
              var alerta = AlertaSanitaria.de(reporte, Instant.now(clock));
              alertas.guardar(alerta);
              TransactionSynchronizationManager.registerSynchronization(
                  new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                      metrics
                          .counter(
                              "avicontrol.alertas_sanitarias.generadas",
                              "prioridad",
                              alerta.prioridad())
                          .increment();
                    }
                  });
              return alerta.id();
            });
  }
}
