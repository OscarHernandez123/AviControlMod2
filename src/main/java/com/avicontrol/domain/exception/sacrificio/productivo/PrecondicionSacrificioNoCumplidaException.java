package com.avicontrol.domain.exception.sacrificio.productivo;

import com.avicontrol.domain.exception.DomainException;

public class PrecondicionSacrificioNoCumplidaException extends DomainException {
  public PrecondicionSacrificioNoCumplidaException() {
    super("El galpón o el lote ya no cumplen las condiciones de sacrificio");
  }
}
