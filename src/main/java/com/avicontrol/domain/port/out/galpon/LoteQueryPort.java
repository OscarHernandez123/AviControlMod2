package com.avicontrol.domain.port.out.galpon;

import com.avicontrol.domain.model.lote.Lote;
import java.util.Optional;
import java.util.UUID;

public interface LoteQueryPort {

  Optional<Lote> buscarActualPorGalponId(UUID galponId);
}
