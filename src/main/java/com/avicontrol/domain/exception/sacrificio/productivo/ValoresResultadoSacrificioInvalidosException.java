package com.avicontrol.domain.exception.sacrificio.productivo;

import com.avicontrol.domain.exception.DomainException;

public class ValoresResultadoSacrificioInvalidosException extends DomainException {
  public ValoresResultadoSacrificioInvalidosException() {
    super("La cantidad y el peso del resultado deben ser positivos; el peso admite dos decimales");
  }
}
