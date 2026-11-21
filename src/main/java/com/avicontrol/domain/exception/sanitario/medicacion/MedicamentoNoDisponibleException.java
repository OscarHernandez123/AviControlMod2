package com.avicontrol.domain.exception.sanitario.medicacion;

import com.avicontrol.domain.exception.DomainException;

public class MedicamentoNoDisponibleException extends DomainException {
  public MedicamentoNoDisponibleException() {
    super("Medicamento sin existencias disponibles");
  }
}
