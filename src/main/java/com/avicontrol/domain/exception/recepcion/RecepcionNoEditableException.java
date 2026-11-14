package com.avicontrol.domain.exception.recepcion;

import com.avicontrol.domain.exception.DomainException;

public class RecepcionNoEditableException extends DomainException {
  public RecepcionNoEditableException() {
    super("La recepción tiene movimientos posteriores confirmados");
  }

  public RecepcionNoEditableException(String message) {
    super(message);
  }
}
