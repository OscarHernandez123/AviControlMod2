package com.avicontrol.domain.exception.sanitario.medicacion;

import com.avicontrol.domain.exception.DomainException;

public class EnfermedadNoDisponibleException extends DomainException {
  public EnfermedadNoDisponibleException() {
    super("Enfermedad no disponible");
  }
}
