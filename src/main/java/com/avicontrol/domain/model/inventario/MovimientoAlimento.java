package com.avicontrol.domain.model.inventario;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record MovimientoAlimento(
    UUID id,
    UUID recepcionId,
    TipoMovimientoInventario tipo,
    BigDecimal cantidadKg,
    EstadoMovimientoInventario estado,
    Instant ocurridoEn,
    String actorId) {
  public MovimientoAlimento {
    Objects.requireNonNull(id);
    Objects.requireNonNull(recepcionId);
    Objects.requireNonNull(tipo);
    Objects.requireNonNull(estado);
    Objects.requireNonNull(ocurridoEn);
    if (cantidadKg == null || cantidadKg.signum() == 0)
      throw new IllegalArgumentException("La cantidad debe ser diferente de cero");
    if (tipo != TipoMovimientoInventario.AJUSTE && cantidadKg.signum() < 0)
      throw new IllegalArgumentException("La cantidad debe ser positiva");
  }

  public BigDecimal efectoEnSaldo() {
    return estado == EstadoMovimientoInventario.ANULADO
        ? BigDecimal.ZERO
        : cantidadKg.multiply(BigDecimal.valueOf(tipo.signo() == 0 ? 1 : tipo.signo()));
  }
}
