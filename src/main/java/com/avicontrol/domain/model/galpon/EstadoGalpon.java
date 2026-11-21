package com.avicontrol.domain.model.galpon;

import java.util.Locale;
import java.util.Optional;

public enum EstadoGalpon {
  DISPONIBLE,
  VACIADO_SANITARIO,
  PRODUCTIVO,
  EN_COSECHA,
  MANTENIMIENTO,
  AISLAMIENTO;

  public static Optional<EstadoGalpon> desde(String valor) {
    if (valor == null) return Optional.empty();
    String normalizado = valor.trim().toUpperCase(Locale.ROOT);
    if (normalizado.equals("PRODUCTIVA")) normalizado = "PRODUCTIVO";
    try {
      return Optional.of(valueOf(normalizado));
    } catch (IllegalArgumentException exception) {
      return Optional.empty();
    }
  }
}
