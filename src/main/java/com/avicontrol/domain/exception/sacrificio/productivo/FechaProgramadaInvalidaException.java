package com.avicontrol.domain.exception.sacrificio.productivo;

import com.avicontrol.domain.exception.DomainException;

public class FechaProgramadaInvalidaException extends DomainException {
  public FechaProgramadaInvalidaException() {
    super("La fecha programada debe ser futura");
  }
}
