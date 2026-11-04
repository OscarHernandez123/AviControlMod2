package com.avicontrol.domain.exception.recepcion;

import com.avicontrol.domain.exception.ConflictException;

public class RecepcionUtilizadaException extends ConflictException {
  public RecepcionUtilizadaException() {
    super("La recepción tiene movimientos posteriores y no puede editarse");
  }
}
