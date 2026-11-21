package com.avicontrol.domain.exception.alimentacion;

import com.avicontrol.domain.exception.DomainException;

public class ExistenciasAlimentoInsuficientesException extends DomainException {
  public ExistenciasAlimentoInsuficientesException() {
    super("No hay alimento vigente suficiente para confirmar el suministro");
  }
}
