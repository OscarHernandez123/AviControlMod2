package com.avicontrol.domain.exception.recepcion;

import com.avicontrol.domain.exception.DomainException;

public class RecursoNoEncontradoException extends DomainException {

  public RecursoNoEncontradoException(String recurso) {
    super(recurso + " no encontrado");
  }
}
