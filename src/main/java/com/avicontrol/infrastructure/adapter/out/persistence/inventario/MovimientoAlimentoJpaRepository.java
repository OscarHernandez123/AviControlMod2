package com.avicontrol.infrastructure.adapter.out.persistence.inventario;

import com.avicontrol.domain.model.inventario.EstadoMovimientoInventario;
import com.avicontrol.domain.model.inventario.TipoMovimientoInventario;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface MovimientoAlimentoJpaRepository extends JpaRepository<MovimientoAlimentoJpaEntity, UUID> {
  Optional<MovimientoAlimentoJpaEntity> findFirstByRecepcionIdAndTipoAndEstado(
      UUID recepcionId, TipoMovimientoInventario tipo, EstadoMovimientoInventario estado);

  boolean existsByRecepcionIdAndEstadoAndTipoNotIn(
      UUID recepcionId,
      EstadoMovimientoInventario estado,
      java.util.Collection<TipoMovimientoInventario> tipos);
}
