package com.avicontrol.domain.exception.sanitario.enfermedad;

import com.avicontrol.domain.exception.DomainException;

public class DatosEnfermedadInvalidosException extends DomainException {
  public DatosEnfermedadInvalidosException(String message) {
    super(message);
  }
}
