package com.avicontrol.domain.exception.sacrificio.productivo;

import com.avicontrol.domain.exception.DomainException;

public class LoteActualNoEncontradoException extends DomainException {
  public LoteActualNoEncontradoException() {
    super("El galpón no tiene un lote actual");
  }
}
