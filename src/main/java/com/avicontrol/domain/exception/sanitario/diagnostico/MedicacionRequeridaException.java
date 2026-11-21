package com.avicontrol.domain.exception.sanitario.diagnostico;

import com.avicontrol.domain.exception.DomainException;

public class MedicacionRequeridaException extends DomainException {
  public MedicacionRequeridaException() {
    super("La enfermedad tratable requiere medicación");
  }
}
