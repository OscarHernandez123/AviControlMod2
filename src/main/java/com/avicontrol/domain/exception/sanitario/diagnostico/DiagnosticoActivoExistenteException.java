package com.avicontrol.domain.exception.sanitario.diagnostico;

import com.avicontrol.domain.exception.DomainException;

public class DiagnosticoActivoExistenteException extends DomainException {
  public DiagnosticoActivoExistenteException() {
    super("El galpón ya tiene un diagnóstico activo");
  }
}
