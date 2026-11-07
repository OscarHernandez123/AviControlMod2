package com.avicontrol.infrastructure.adapter.out.integration.modulo1;

import com.avicontrol.domain.exception.galpon.InformacionGalponNoDisponibleException;
import com.avicontrol.domain.port.out.modulo1.GalponUpdatePort;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!local & !test")
public class GalponUpdateNoDisponibleAdapter implements GalponUpdatePort {
  @Override
  public boolean aislarSiProductiva(UUID galponId) {
    throw new InformacionGalponNoDisponibleException();
  }

  @Override
  public boolean pasarACosechaSiProductiva(UUID galponId, UUID loteId) {
    throw new InformacionGalponNoDisponibleException();
  }

  @Override
  public boolean vaciarSiEnCosecha(UUID galponId, UUID loteId) {
    throw new InformacionGalponNoDisponibleException();
  }

  @Override
  public boolean vaciarSiAislado(UUID galponId, UUID loteId) {
    throw new InformacionGalponNoDisponibleException();
  }
}
