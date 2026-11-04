package com.avicontrol.infrastructure.adapter.out.persistence.inventario;

import com.avicontrol.domain.model.inventario.EstadoMovimientoInventario;
import com.avicontrol.domain.model.inventario.TipoMovimientoInventario;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "movimiento_medicamento")
class MovimientoMedicamentoJpaEntity {
  @Id UUID id;

  @Column(name = "recepcion_id", nullable = false)
  UUID recepcionId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  TipoMovimientoInventario tipo;

  @Column(name = "cantidad_unidad_base", nullable = false, precision = 19, scale = 4)
  BigDecimal cantidadUnidadBase;

  @Column(name = "unidad_base", nullable = false, length = 20)
  String unidadBase;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  EstadoMovimientoInventario estado;

  @Column(name = "ocurrido_en", nullable = false)
  Instant ocurridoEn;

  @Column(name = "actor_id")
  String actorId;

  protected MovimientoMedicamentoJpaEntity() {}

  MovimientoMedicamentoJpaEntity(
      UUID id,
      UUID recepcionId,
      TipoMovimientoInventario tipo,
      BigDecimal cantidad,
      String unidad,
      EstadoMovimientoInventario estado,
      Instant ocurridoEn,
      String actor) {
    this.id = id;
    this.recepcionId = recepcionId;
    this.tipo = tipo;
    this.cantidadUnidadBase = cantidad;
    this.unidadBase = unidad;
    this.estado = estado;
    this.ocurridoEn = ocurridoEn;
    this.actorId = actor;
  }
}
