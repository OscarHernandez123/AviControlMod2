package com.avicontrol.infrastructure.adapter.out.integration.modulo1;

import com.avicontrol.domain.exception.sanitario.aislamiento.Modulo1NoDisponibleException;
import com.avicontrol.domain.port.out.sanitario.medicacion.GalponLoteQueryPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!local & !test")
public class GalponLoteNoDisponibleAdapter implements GalponLoteQueryPort {
  @Override
  public Optional<GalponLote> consultar(UUID galponId) {
    throw new Modulo1NoDisponibleException();
  }
}
