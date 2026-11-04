package com.avicontrol.domain.port.out.inventario;

import com.avicontrol.domain.model.inventario.MovimientoAlimento;
import java.util.Optional;
import java.util.UUID;

public interface MovimientoAlimentoRepositoryPort {
  MovimientoAlimento guardar(MovimientoAlimento movimiento);

  Optional<MovimientoAlimento> buscarEntradaConfirmada(UUID recepcionId);
}
