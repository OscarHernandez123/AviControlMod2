package com.avicontrol.domain.exception.recepcion;

import com.avicontrol.domain.exception.DomainException;

public class FechaVencimientoInvalidaException extends DomainException {

  public FechaVencimientoInvalidaException() {
    super("La fecha de vencimiento debe ser posterior a la fecha de ingreso");
  }
}
