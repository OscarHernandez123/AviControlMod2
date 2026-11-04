package com.avicontrol.domain.model.inventario;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record BodegaCentral(UUID id, BigDecimal capacidadMaximaKg) {
  public BodegaCentral {
    Objects.requireNonNull(id, "El id de la bodega es obligatorio");
    if (capacidadMaximaKg == null || capacidadMaximaKg.signum() <= 0)
      throw new IllegalArgumentException("La capacidad debe ser positiva");
  }
}
