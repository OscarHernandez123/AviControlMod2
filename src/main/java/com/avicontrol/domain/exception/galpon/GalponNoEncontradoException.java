package com.avicontrol.domain.exception.galpon;

import com.avicontrol.domain.exception.ResourceNotFoundException;

public final class GalponNoEncontradoException extends ResourceNotFoundException {

  public GalponNoEncontradoException() {
    super("No existe el galpón solicitado");
  }

  @Override
  public String code() {
    return "GALPON_NO_ENCONTRADO";
  }
}
