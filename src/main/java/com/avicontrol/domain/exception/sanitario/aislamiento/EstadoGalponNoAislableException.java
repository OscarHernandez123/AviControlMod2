package com.avicontrol.domain.exception.sanitario.aislamiento;

import com.avicontrol.domain.exception.DomainException;

public class EstadoGalponNoAislableException extends DomainException {
  public EstadoGalponNoAislableException(String message) {
    super(message);
  }
}
