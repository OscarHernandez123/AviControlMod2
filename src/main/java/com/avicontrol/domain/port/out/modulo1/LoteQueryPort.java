package com.avicontrol.domain.port.out.modulo1;

import com.avicontrol.domain.model.lote.Lote;
import java.util.Optional;
import java.util.UUID;

public interface LoteQueryPort {
  Optional<Lote> consultarActual(UUID galponId);
}
