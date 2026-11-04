package com.avicontrol.domain.exception.sanitario;

import com.avicontrol.domain.exception.ResourceNotFoundException;

public final class EnfermedadNoEncontradaException extends ResourceNotFoundException {

  public EnfermedadNoEncontradaException() {
    super("No existe la enfermedad solicitada");
  }

  @Override
  public String code() {
    return "ENFERMEDAD_NO_ENCONTRADA";
  }
}
