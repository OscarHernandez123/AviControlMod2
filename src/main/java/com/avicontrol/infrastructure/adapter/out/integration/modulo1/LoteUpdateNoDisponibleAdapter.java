package com.avicontrol.infrastructure.adapter.out.integration.modulo1;

import com.avicontrol.domain.exception.galpon.InformacionGalponNoDisponibleException;
import com.avicontrol.domain.port.out.modulo1.LoteUpdatePort;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!local & !test")
public class LoteUpdateNoDisponibleAdapter implements LoteUpdatePort {
  @Override
  public boolean finalizarAlojamiento(UUID galponId, UUID loteId) {
    throw new InformacionGalponNoDisponibleException();
  }

  @Override
  public boolean sacrificarPoblacionSiPositiva(UUID galponId, UUID loteId) {
    throw new InformacionGalponNoDisponibleException();
  }
}
