package com.avicontrol.domain.exception.inventario;

import com.avicontrol.domain.exception.BusinessRuleViolationException;

public final class MovimientoInvalidoException extends BusinessRuleViolationException {
  public MovimientoInvalidoException(String message) {
    super("MOVIMIENTO_INVALIDO", message);
  }
}
