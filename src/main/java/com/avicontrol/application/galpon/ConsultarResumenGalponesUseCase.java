package com.avicontrol.application.galpon;

import com.avicontrol.application.galpon.result.ResumenGalponesResult;
import com.avicontrol.domain.model.galpon.EstadoGalpon;
import com.avicontrol.domain.model.galpon.ResumenGalpones;
import com.avicontrol.domain.port.out.modulo1.GalponQueryPort;
import java.util.LinkedHashMap;

public class ConsultarResumenGalponesUseCase {
  private final GalponQueryPort galpones;

  public ConsultarResumenGalponesUseCase(GalponQueryPort galpones) {
    this.galpones = galpones;
  }

  public ResumenGalponesResult consultar() {
    ResumenGalpones resumen =
        ResumenGalpones.contar(
            galpones.todos().stream().map(GalponQueryPort.GalponDatos::estado).toList());
    var conteos = new LinkedHashMap<String, Integer>();
    for (EstadoGalpon estado : EstadoGalpon.values())
      conteos.put(estado.name(), resumen.porEstado().get(estado));
    return new ResumenGalponesResult(
        resumen.totalRegistrado(),
        resumen.totalValido(),
        resumen.registrosInconsistentes(),
        conteos);
  }
}
