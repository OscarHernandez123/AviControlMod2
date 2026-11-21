package com.avicontrol.domain.exception.medicamento;

import com.avicontrol.domain.exception.DomainException;

public class ExistenciasMedicamentoInsuficientesException extends DomainException {
  public ExistenciasMedicamentoInsuficientesException() {
    super("Existencias de medicamento insuficientes");
  }
}
