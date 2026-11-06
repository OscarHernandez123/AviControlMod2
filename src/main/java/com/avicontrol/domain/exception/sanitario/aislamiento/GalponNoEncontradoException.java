package com.avicontrol.domain.exception.sanitario.aislamiento;

import com.avicontrol.domain.exception.DomainException;

public class GalponNoEncontradoException extends DomainException {
  public GalponNoEncontradoException() {
    super("Galpón no encontrado");
  }
}
