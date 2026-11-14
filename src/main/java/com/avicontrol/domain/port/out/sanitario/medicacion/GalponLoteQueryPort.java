package com.avicontrol.domain.port.out.sanitario.medicacion;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface GalponLoteQueryPort {
  Optional<GalponLote> consultar(UUID galponId);

  default Optional<GalponLote> bloquear(UUID galponId) {
    return consultar(galponId);
  }

  record GalponLote(
      UUID galponId,
      String nombre,
      int aforoMaximo,
      String estado,
      UUID loteId,
      String loteCodigo,
      int poblacionViva,
      LocalDate fechaIngreso) {}
}
