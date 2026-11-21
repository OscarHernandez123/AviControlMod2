package com.avicontrol.domain.exception.sanitario.diagnostico;

import com.avicontrol.domain.exception.DomainException;

public class MedicacionNoCompatibleException extends DomainException {
  public MedicacionNoCompatibleException() {
    super("La medicación no corresponde a la enfermedad");
  }
}
