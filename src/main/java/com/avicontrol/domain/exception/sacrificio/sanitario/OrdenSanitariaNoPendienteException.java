package com.avicontrol.domain.exception.sacrificio.sanitario;

import com.avicontrol.domain.exception.DomainException;

public class OrdenSanitariaNoPendienteException extends DomainException {
  public OrdenSanitariaNoPendienteException() {
    super("La orden sanitaria ya fue ejecutada o modificada");
  }
}
