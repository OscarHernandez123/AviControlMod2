package com.avicontrol.domain.exception.sanitario.medicacion;

import com.avicontrol.domain.exception.DomainException;

public class DatosMedicacionInvalidosException extends DomainException {
  public DatosMedicacionInvalidosException(String message) {
    super(message);
  }
}
