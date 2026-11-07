package com.avicontrol.domain.exception.sacrificio.sanitario;

import com.avicontrol.domain.exception.DomainException;

public class PoblacionNoSacrificableException extends DomainException {
  public PoblacionNoSacrificableException() {
    super("El lote no tiene población sacrificable vigente");
  }
}
