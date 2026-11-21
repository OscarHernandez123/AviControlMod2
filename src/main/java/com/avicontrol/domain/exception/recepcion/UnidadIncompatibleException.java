package com.avicontrol.domain.exception.recepcion;

import com.avicontrol.domain.exception.DomainException;

public class UnidadIncompatibleException extends DomainException {

  public UnidadIncompatibleException() {
    super("La unidad no es compatible con la unidad base del medicamento");
  }
}
