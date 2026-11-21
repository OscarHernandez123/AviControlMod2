package com.avicontrol.domain.exception.alimentacion;

import com.avicontrol.domain.exception.DomainException;

public class LoteActivoNoEncontradoException extends DomainException {
  public LoteActivoNoEncontradoException() {
    super("No hay un lote activo con población para suministrar alimento");
  }
}
