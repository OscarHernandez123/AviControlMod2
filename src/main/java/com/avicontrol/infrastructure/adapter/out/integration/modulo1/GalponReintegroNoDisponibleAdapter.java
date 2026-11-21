package com.avicontrol.infrastructure.adapter.out.integration.modulo1;

import com.avicontrol.domain.exception.sanitario.aislamiento.Modulo1NoDisponibleException;
import com.avicontrol.domain.port.out.sanitario.diagnostico.GalponReintegroPort;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!local & !test")
public class GalponReintegroNoDisponibleAdapter implements GalponReintegroPort {
  @Override
  public boolean reintegrarSiAislado(UUID galponId, UUID loteId) {
    throw new Modulo1NoDisponibleException();
  }
}
