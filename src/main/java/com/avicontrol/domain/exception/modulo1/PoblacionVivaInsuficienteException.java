package com.avicontrol.domain.exception.modulo1;

import com.avicontrol.domain.exception.DomainException;

public class PoblacionVivaInsuficienteException extends DomainException {
  public PoblacionVivaInsuficienteException() {
    super("Población viva insuficiente");
  }
}
