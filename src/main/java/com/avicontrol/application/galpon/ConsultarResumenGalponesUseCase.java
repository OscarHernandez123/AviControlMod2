package com.avicontrol.application.galpon;

import com.avicontrol.application.galpon.result.ResumenGalponesResult;
import com.avicontrol.application.support.CurrentActorProvider;
import com.avicontrol.domain.exception.ConflictException;
import com.avicontrol.domain.model.galpon.EstadoGalpon;
import com.avicontrol.domain.port.out.galpon.GalponQueryPort;
import com.avicontrol.domain.port.out.galpon.ResumenGalponesFuente;
import java.util.EnumMap;
import java.util.Map;

public final class ConsultarResumenGalponesUseCase {

  private final GalponQueryPort galponQueryPort;
  private final CurrentActorProvider currentActorProvider;

  public ConsultarResumenGalponesUseCase(
      GalponQueryPort galponQueryPort, CurrentActorProvider currentActorProvider) {
    this.galponQueryPort = galponQueryPort;
    this.currentActorProvider = currentActorProvider;
  }

  public ResumenGalponesResult execute() {
    GalponAuthorization.requireAdministrator(currentActorProvider.currentActor());
    ResumenGalponesFuente fuente = galponQueryPort.consultarResumen();
    Map<EstadoGalpon, Long> porEstado = new EnumMap<>(EstadoGalpon.class);
    for (EstadoGalpon estado : EstadoGalpon.values()) {
      long conteo = fuente.porEstado().getOrDefault(estado, 0L);
      if (conteo < 0) {
        throw new ConflictException("Los conteos de galpones son incoherentes");
      }
      porEstado.put(estado, conteo);
    }
    long totalValido = porEstado.values().stream().mapToLong(Long::longValue).sum();
    if (fuente.totalRegistrado() != totalValido + fuente.registrosInconsistentes()) {
      throw new ConflictException("Los totales de galpones son incoherentes");
    }
    return new ResumenGalponesResult(
        fuente.totalRegistrado(),
        totalValido,
        fuente.registrosInconsistentes(),
        Map.copyOf(porEstado));
  }
}
