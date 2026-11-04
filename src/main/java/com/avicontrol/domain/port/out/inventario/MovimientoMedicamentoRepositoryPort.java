package com.avicontrol.domain.port.out.inventario;

import com.avicontrol.domain.model.inventario.MovimientoMedicamento;
import java.util.Optional;
import java.util.UUID;

public interface MovimientoMedicamentoRepositoryPort {
  MovimientoMedicamento guardar(MovimientoMedicamento movimiento);

  Optional<MovimientoMedicamento> buscarEntradaConfirmada(UUID recepcionId);

  default boolean tieneMovimientosPosterioresConfirmados(UUID recepcionId) {
    return false;
  }
}
