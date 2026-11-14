package com.avicontrol.infrastructure.adapter.out.integration.modulo1;

import com.avicontrol.domain.exception.sanitario.aislamiento.Modulo1NoDisponibleException;
import com.avicontrol.domain.model.sanitario.aislamiento.GalponLoteAislamiento;
import com.avicontrol.domain.port.out.sanitario.aislamiento.GalponLoteAislamientoQueryPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!local & !test")
public class GalponNoDisponibleAdapter implements GalponLoteAislamientoQueryPort {
  @Override
  public Optional<GalponLoteAislamiento> consultar(UUID id) {
    throw new Modulo1NoDisponibleException();
  }
}
