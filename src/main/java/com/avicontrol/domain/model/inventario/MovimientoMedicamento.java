package com.avicontrol.domain.model.inventario;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record MovimientoMedicamento(
    UUID id,
    UUID recepcionId,
    TipoMovimientoInventario tipo,
    BigDecimal cantidadUnidadBase,
    String unidadBase,
    EstadoMovimientoInventario estado,
    Instant ocurridoEn,
    String actorId) {
  public MovimientoMedicamento {
    Objects.requireNonNull(id);
    Objects.requireNonNull(recepcionId);
    Objects.requireNonNull(tipo);
    Objects.requireNonNull(estado);
    Objects.requireNonNull(ocurridoEn);
    if (!("GRAMO".equals(unidadBase)
        || "MILILITRO".equals(unidadBase)
        || "UNIDAD".equals(unidadBase)))
      throw new IllegalArgumentException("La unidad base no es válida");
    if (cantidadUnidadBase == null || cantidadUnidadBase.signum() == 0)
      throw new IllegalArgumentException("La cantidad debe ser diferente de cero");
    if (tipo != TipoMovimientoInventario.AJUSTE && cantidadUnidadBase.signum() < 0)
      throw new IllegalArgumentException("La cantidad debe ser positiva");
  }

  public BigDecimal efectoEnSaldo() {
    return estado == EstadoMovimientoInventario.ANULADO
        ? BigDecimal.ZERO
        : cantidadUnidadBase.multiply(BigDecimal.valueOf(tipo.signo() == 0 ? 1 : tipo.signo()));
  }
}
