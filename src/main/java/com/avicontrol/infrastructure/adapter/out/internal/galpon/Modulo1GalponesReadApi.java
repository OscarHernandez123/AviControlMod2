package com.avicontrol.infrastructure.adapter.out.internal.galpon;

import com.avicontrol.domain.model.galpon.Galpon;
import com.avicontrol.domain.model.lote.Lote;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Public read contract temporarily implemented in memory until Module 1 is connected. */
public interface Modulo1GalponesReadApi {

  Optional<Galpon> findGalponById(UUID galponId);

  List<Galpon> findAllGalpones();

  List<Lote> findCurrentLotesByGalponId(UUID galponId);

  long inconsistentGalponRecords();
}
