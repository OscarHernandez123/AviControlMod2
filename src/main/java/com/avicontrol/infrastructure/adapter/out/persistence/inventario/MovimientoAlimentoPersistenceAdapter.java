package com.avicontrol.infrastructure.adapter.out.persistence.inventario;

import com.avicontrol.domain.model.inventario.*;
import com.avicontrol.domain.port.out.inventario.MovimientoAlimentoRepositoryPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class MovimientoAlimentoPersistenceAdapter implements MovimientoAlimentoRepositoryPort {
  private final MovimientoAlimentoJpaRepository repo;
  private final InventarioPersistenceMapper mapper;

  MovimientoAlimentoPersistenceAdapter(
      MovimientoAlimentoJpaRepository repo, InventarioPersistenceMapper mapper) {
    this.repo = repo;
    this.mapper = mapper;
  }

  public MovimientoAlimento guardar(MovimientoAlimento m) {
    return mapper.toDomain(repo.save(mapper.toEntity(m)));
  }

  public Optional<MovimientoAlimento> buscarEntradaConfirmada(UUID id) {
    return repo.findFirstByRecepcionIdAndTipoAndEstado(
            id, TipoMovimientoInventario.ENTRADA, EstadoMovimientoInventario.CONFIRMADO)
        .map(mapper::toDomain);
  }

  public boolean tieneMovimientosPosterioresConfirmados(UUID id) {
    return repo.existsByRecepcionIdAndEstadoAndTipoNotIn(
        id,
        EstadoMovimientoInventario.CONFIRMADO,
        List.of(TipoMovimientoInventario.ENTRADA, TipoMovimientoInventario.AJUSTE));
  }
}
