package com.avicontrol.domain.exception.sacrificio.productivo;

import com.avicontrol.domain.exception.DomainException;

public class OrdenNoModificableException extends DomainException {
  public OrdenNoModificableException() {
    super("La orden no permite esta transición");
  }
}
