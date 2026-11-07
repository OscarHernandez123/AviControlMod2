package com.avicontrol.domain.exception.sacrificio.productivo;

import com.avicontrol.domain.exception.DomainException;

public class ResultadoSacrificioNoModificableException extends DomainException {
  public ResultadoSacrificioNoModificableException() {
    super("El resultado ya fue utilizado o modificado concurrentemente");
  }
}
