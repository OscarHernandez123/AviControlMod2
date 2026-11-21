package com.avicontrol.domain.exception.medicamento;

import com.avicontrol.domain.exception.DomainException;

public class UnidadMedicamentoIncompatibleException extends DomainException {
  public UnidadMedicamentoIncompatibleException() {
    super("Unidad incompatible con el medicamento");
  }
}
