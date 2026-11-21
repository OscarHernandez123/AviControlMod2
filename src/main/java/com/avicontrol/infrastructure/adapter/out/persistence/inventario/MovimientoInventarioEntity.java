package com.avicontrol.infrastructure.adapter.out.persistence.inventario;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "movimientos_inventario")
public class MovimientoInventarioEntity {
  @Id private UUID id;
  private UUID recepcionId;
  private UUID productoId;
  private String tipoProducto;
  private String tipoMovimiento;
  private String estado;
  private BigDecimal cantidad;
  private String unidad;
  private String actor;
  private Instant fechaRegistro;

  protected MovimientoInventarioEntity() {}

  public MovimientoInventarioEntity(
      UUID recepcionId,
      UUID productoId,
      String tipoProducto,
      BigDecimal cantidad,
      String unidad,
      String actor) {
    id = UUID.randomUUID();
    this.recepcionId = recepcionId;
    this.productoId = productoId;
    this.tipoProducto = tipoProducto;
    tipoMovimiento = "ENTRADA";
    estado = "CONFIRMADO";
    this.cantidad = cantidad;
    this.unidad = unidad;
    this.actor = actor;
    fechaRegistro = Instant.now();
  }

  public void ajustarEntrada(UUID productoId, BigDecimal cantidad, String unidad, String actor) {
    this.productoId = productoId;
    this.cantidad = cantidad;
    this.unidad = unidad;
    this.actor = actor;
    fechaRegistro = Instant.now();
  }
}
