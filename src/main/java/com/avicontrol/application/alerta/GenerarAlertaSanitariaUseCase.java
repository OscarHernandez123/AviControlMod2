package com.avicontrol.application.alerta;

import com.avicontrol.domain.model.alerta.AlertaSanitaria;
import com.avicontrol.domain.model.mortalidad.RegistroMortalidadAnormal;
import com.avicontrol.domain.port.out.alerta.AlertaSanitariaRepositoryPort;
import com.avicontrol.domain.port.out.mortalidad.GenerarAlertaSanitariaPort;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class GenerarAlertaSanitariaUseCase implements GenerarAlertaSanitariaPort {
  private final AlertaSanitariaRepositoryPort alertas;
  private final Clock clock;

  public GenerarAlertaSanitariaUseCase(AlertaSanitariaRepositoryPort alertas, Clock clock) {
    this.alertas = alertas;
    this.clock = clock;
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
              return alerta.id();
            });
  }
}
