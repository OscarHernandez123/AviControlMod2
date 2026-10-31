package com.avicontrol.domain.model.galpon;

import java.util.Objects;
import java.util.UUID;

public record Galpon(UUID id, String nombre, int aforoMaximo, EstadoGalpon estado) {

  public Galpon {
    Objects.requireNonNull(id, "El id del galpón es obligatorio");
    if (nombre == null || nombre.isBlank()) {
      throw new IllegalArgumentException("El nombre del galpón es obligatorio");
    }
    if (aforoMaximo <= 0) {
      throw new IllegalArgumentException("El aforo máximo debe ser positivo");
    }
    Objects.requireNonNull(estado, "El estado del galpón es obligatorio");
  }
}
