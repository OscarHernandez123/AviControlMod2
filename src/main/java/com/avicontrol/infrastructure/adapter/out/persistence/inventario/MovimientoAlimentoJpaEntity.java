package com.avicontrol.infrastructure.adapter.out.persistence.inventario;

import com.avicontrol.domain.model.inventario.EstadoMovimientoInventario;
import com.avicontrol.domain.model.inventario.TipoMovimientoInventario;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "movimiento_alimento")
class MovimientoAlimentoJpaEntity {
  @Id UUID id;

  @Column(name = "recepcion_id", nullable = false)
  UUID recepcionId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  TipoMovimientoInventario tipo;

  @Column(name = "cantidad_kg", nullable = false, precision = 19, scale = 4)
  BigDecimal cantidadKg;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  EstadoMovimientoInventario estado;

  @Column(name = "ocurrido_en", nullable = false)
  Instant ocurridoEn;

  @Column(name = "actor_id")
  String actorId;

  protected MovimientoAlimentoJpaEntity() {}

  MovimientoAlimentoJpaEntity(
      UUID id,
      UUID recepcionId,
      TipoMovimientoInventario tipo,
      BigDecimal cantidadKg,
      EstadoMovimientoInventario estado,
      Instant ocurridoEn,
      String actorId) {
    this.id = id;
    this.recepcionId = recepcionId;
    this.tipo = tipo;
    this.cantidadKg = cantidadKg;
    this.estado = estado;
    this.ocurridoEn = ocurridoEn;
    this.actorId = actorId;
  }
}
