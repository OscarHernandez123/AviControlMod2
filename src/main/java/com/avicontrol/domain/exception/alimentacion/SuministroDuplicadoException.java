package com.avicontrol.domain.exception.alimentacion;

import com.avicontrol.domain.exception.DomainException;

public class SuministroDuplicadoException extends DomainException {
  public SuministroDuplicadoException() {
    super("La clave de idempotencia ya fue utilizada con otro suministro");
  }
}
