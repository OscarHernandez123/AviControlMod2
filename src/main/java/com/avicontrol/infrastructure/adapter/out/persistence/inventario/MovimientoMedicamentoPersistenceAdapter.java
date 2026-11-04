package com.avicontrol.infrastructure.adapter.out.persistence.inventario;

import com.avicontrol.domain.model.inventario.*;
import com.avicontrol.domain.port.out.inventario.MovimientoMedicamentoRepositoryPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class MovimientoMedicamentoPersistenceAdapter implements MovimientoMedicamentoRepositoryPort {
  private final MovimientoMedicamentoJpaRepository repo;
  private final InventarioPersistenceMapper mapper;

  MovimientoMedicamentoPersistenceAdapter(
      MovimientoMedicamentoJpaRepository repo, InventarioPersistenceMapper mapper) {
    this.repo = repo;
    this.mapper = mapper;
  }

  public MovimientoMedicamento guardar(MovimientoMedicamento m) {
    return mapper.toDomain(repo.save(mapper.toEntity(m)));
  }

  public Optional<MovimientoMedicamento> buscarEntradaConfirmada(UUID id) {
    return repo.findFirstByRecepcionIdAndTipoAndEstado(
            id, TipoMovimientoInventario.ENTRADA, EstadoMovimientoInventario.CONFIRMADO)
        .map(mapper::toDomain);
  }
}
