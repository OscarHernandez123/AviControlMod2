package com.avicontrol.domain.exception.modulo1;

import com.avicontrol.domain.exception.DomainException;

public class ReferenciaLoteInconsistenteException extends DomainException {
  public ReferenciaLoteInconsistenteException() {
    super("Lote, galpón o versión inconsistente");
  }
}
