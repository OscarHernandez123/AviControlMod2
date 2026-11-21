package com.avicontrol.domain.exception.galpon;

import com.avicontrol.domain.exception.DomainException;

public class LoteActualInconsistenteException extends DomainException {
  public LoteActualInconsistenteException() {
    super("Los datos del lote actual son inconsistentes");
  }
}
