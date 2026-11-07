package com.avicontrol.domain.exception.sacrificio.productivo;

import com.avicontrol.domain.exception.DomainException;

public class ResultadoSacrificioDuplicadoException extends DomainException {
  public ResultadoSacrificioDuplicadoException() {
    super("Ya existe un resultado para el lote");
  }
}
