package com.avicontrol.domain.exception.sanitario.aislamiento;

import com.avicontrol.domain.exception.DomainException;

public class SolicitudAislamientoInvalidaException extends DomainException {
  public SolicitudAislamientoInvalidaException(String message) {
    super(message);
  }
}
