package com.avicontrol.application.modulo1;

import com.avicontrol.application.modulo1.result.InventarioVivoActualizadoResult;
import com.avicontrol.domain.port.out.modulo1.ActualizarInventarioVivoPort;
import org.springframework.transaction.annotation.Transactional;

public class ActualizarInventarioVivoUseCase {
  private final ActualizarInventarioVivoPort port;

  public ActualizarInventarioVivoUseCase(ActualizarInventarioVivoPort port) {
    this.port = port;
  }

  @Transactional
  public InventarioVivoActualizadoResult actualizar(ActualizarInventarioVivoCommand command) {
    if (command == null) throw new IllegalArgumentException("Comando obligatorio");
    var r =
        port.descontar(
            new ActualizarInventarioVivoPort.Descuento(
                command.loteId(),
                command.galponId(),
                command.cantidadBajas(),
                command.causa(),
                command.actor(),
                command.versionEsperada(),
                command.idempotencyKey()));
    return new InventarioVivoActualizadoResult(
        r.loteId(), r.poblacionAnterior(), r.variacion(), r.poblacionResultante(), r.causa());
  }
}
