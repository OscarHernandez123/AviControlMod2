package com.avicontrol.domain.exception.sanitario.medicacion;

import com.avicontrol.domain.exception.DomainException;

public class MedicacionNoEncontradaException extends DomainException {
  public MedicacionNoEncontradaException() {
    super("Medicación no encontrada");
  }
}
