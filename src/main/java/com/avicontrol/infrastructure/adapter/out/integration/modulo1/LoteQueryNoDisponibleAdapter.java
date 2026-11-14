package com.avicontrol.infrastructure.adapter.out.integration.modulo1;

import com.avicontrol.domain.exception.galpon.InformacionGalponNoDisponibleException;
import com.avicontrol.domain.model.lote.Lote;
import com.avicontrol.domain.port.out.modulo1.LoteQueryPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!local & !test")
public class LoteQueryNoDisponibleAdapter implements LoteQueryPort {
  @Override
  public Optional<Lote> consultarActual(UUID galponId) {
    throw new InformacionGalponNoDisponibleException();
  }
}
