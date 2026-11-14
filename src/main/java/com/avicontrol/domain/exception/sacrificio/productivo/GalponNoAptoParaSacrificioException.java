package com.avicontrol.domain.exception.sacrificio.productivo;

import com.avicontrol.domain.exception.DomainException;

public class GalponNoAptoParaSacrificioException extends DomainException {
  public GalponNoAptoParaSacrificioException() {
    super("El galpón cambió de estado o de lote durante la validación");
  }
}
