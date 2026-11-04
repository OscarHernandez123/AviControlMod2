package com.avicontrol.infrastructure.adapter.out.persistence.inventario;

import com.avicontrol.domain.model.inventario.EstadoMovimientoInventario;
import com.avicontrol.domain.model.inventario.TipoMovimientoInventario;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface MovimientoMedicamentoJpaRepository
    extends JpaRepository<MovimientoMedicamentoJpaEntity, UUID> {
  Optional<MovimientoMedicamentoJpaEntity> findFirstByRecepcionIdAndTipoAndEstado(
      UUID recepcionId, TipoMovimientoInventario tipo, EstadoMovimientoInventario estado);

  boolean existsByRecepcionIdAndEstadoAndTipoNotIn(
      UUID recepcionId,
      EstadoMovimientoInventario estado,
      java.util.Collection<TipoMovimientoInventario> tipos);
}
