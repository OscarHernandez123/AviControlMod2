package com.avicontrol.infrastructure.adapter.out.persistence.inventario;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovimientoInventarioJpaRepository
    extends JpaRepository<MovimientoInventarioEntity, UUID> {
  @Query(
      "select count(m) from MovimientoInventarioEntity m where m.recepcionId = :id and m.tipoMovimiento <> 'ENTRADA' and m.estado = 'CONFIRMADO'")
  long contarPosteriores(@Param("id") UUID id);

  @Query(
      "select m from MovimientoInventarioEntity m where m.recepcionId = :id and m.tipoProducto = :tipo and m.tipoMovimiento = 'ENTRADA'")
  Optional<MovimientoInventarioEntity> entradaInicial(
      @Param("id") UUID id, @Param("tipo") String tipo);
}
