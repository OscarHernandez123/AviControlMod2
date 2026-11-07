package com.avicontrol.domain.exception.sanitario.diagnostico;

import com.avicontrol.domain.exception.DomainException;

public class DiagnosticoActivoExistenteException extends DomainException {
  public DiagnosticoActivoExistenteException() {
    super("Ya existe un diagnóstico activo incompatible para el galpón");
  }
}
