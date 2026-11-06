package com.avicontrol.domain.exception.sanitario.enfermedad;

import com.avicontrol.domain.exception.DomainException;

public class EnfermedadNoEncontradaException extends DomainException {
  public EnfermedadNoEncontradaException() {
    super("Enfermedad no encontrada");
  }
}
