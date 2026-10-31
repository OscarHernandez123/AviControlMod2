package com.avicontrol.domain.exception.galpon;

import com.avicontrol.domain.exception.ConflictException;

public final class FechaIngresoInconsistenteException extends ConflictException {

  public FechaIngresoInconsistenteException() {
    super("La fecha de ingreso del lote es posterior a la fecha de consulta");
  }

  @Override
  public String code() {
    return "FECHA_INGRESO_INCONSISTENTE";
  }
}
