package com.avicontrol.domain.exception.medicamento;

import com.avicontrol.domain.exception.DomainException;

public class CantidadMedicamentoInvalidaException extends DomainException {
  public CantidadMedicamentoInvalidaException(String message) {
    super(message);
  }
}
