package com.avicontrol.infrastructure.adapter.out.integration.modulo1;

import com.avicontrol.domain.exception.galpon.InformacionGalponNoDisponibleException;
import com.avicontrol.domain.port.out.modulo1.ActualizarInventarioVivoPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!local & !test")
public class InventarioVivoNoDisponibleAdapter implements ActualizarInventarioVivoPort {
  @Override
  public ActualizarInventarioVivoPort.Resultado descontar(
      ActualizarInventarioVivoPort.Descuento command) {
    throw new InformacionGalponNoDisponibleException();
  }
}
