package com.avicontrol.domain.port.out.galpon;

import com.avicontrol.domain.model.galpon.Galpon;
import java.util.Optional;
import java.util.UUID;

public interface GalponQueryPort {

  Optional<Galpon> buscarPorId(UUID galponId);

  GalponPage listar(GalponPageQuery query);

  ResumenGalponesFuente consultarResumen();
}
