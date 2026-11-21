package com.avicontrol.domain.exception.galpon;

import com.avicontrol.domain.exception.DomainException;

public class FechaIngresoInconsistenteException extends DomainException {
  public FechaIngresoInconsistenteException() {
    super("La fecha de ingreso del lote es inconsistente");
  }
}
