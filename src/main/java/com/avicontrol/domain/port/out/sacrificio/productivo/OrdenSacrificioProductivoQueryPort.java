package com.avicontrol.domain.port.out.sacrificio.productivo;

import com.avicontrol.domain.model.sacrificio.productivo.OrdenSacrificioProductivo;
import java.util.Optional;
import java.util.UUID;

public interface OrdenSacrificioProductivoQueryPort {
  Optional<OrdenSacrificioProductivo> buscar(UUID ordenId);
}
