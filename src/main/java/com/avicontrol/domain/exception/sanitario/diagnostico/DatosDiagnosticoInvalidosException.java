package com.avicontrol.domain.exception.sanitario.diagnostico;

import com.avicontrol.domain.exception.DomainException;

public class DatosDiagnosticoInvalidosException extends DomainException {
  public DatosDiagnosticoInvalidosException(String message) {
    super(message);
  }
}
