package com.avicontrol.domain.exception.inventario;

import com.avicontrol.domain.exception.ResourceNotFoundException;

public final class RecepcionNoEncontradaException extends ResourceNotFoundException {
  public RecepcionNoEncontradaException() {
    super("No existe la recepción de inventario");
  }

  @Override
  public String code() {
    return "RECEPCION_NO_ENCONTRADA";
  }
}
