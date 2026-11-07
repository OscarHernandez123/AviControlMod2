package com.avicontrol.domain.exception.medicamento;

import com.avicontrol.domain.exception.DomainException;

public class TratamientoNoActivoException extends DomainException {
  public TratamientoNoActivoException() {
    super("El diagnóstico no tiene tratamiento activo");
  }
}
