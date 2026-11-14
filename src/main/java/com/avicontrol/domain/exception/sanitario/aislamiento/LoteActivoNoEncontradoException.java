package com.avicontrol.domain.exception.sanitario.aislamiento;

import com.avicontrol.domain.exception.DomainException;

public class LoteActivoNoEncontradoException extends DomainException {
  public LoteActivoNoEncontradoException() {
    super("Lote activo no encontrado");
  }
}
