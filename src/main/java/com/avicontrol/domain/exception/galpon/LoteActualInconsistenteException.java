package com.avicontrol.domain.exception.galpon;

import com.avicontrol.domain.exception.ConflictException;

public final class LoteActualInconsistenteException extends ConflictException {

  public LoteActualInconsistenteException() {
    super("El galpón tiene más de un lote actualmente alojado");
  }

  @Override
  public String code() {
    return "LOTE_ACTUAL_INCONSISTENTE";
  }
}
