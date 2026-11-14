package com.avicontrol.domain.exception.medicamento;

import com.avicontrol.domain.exception.DomainException;

public class ConsumoMedicamentoNoEncontradoException extends DomainException {
  public ConsumoMedicamentoNoEncontradoException() {
    super("Consumo de medicamento no encontrado");
  }
}
