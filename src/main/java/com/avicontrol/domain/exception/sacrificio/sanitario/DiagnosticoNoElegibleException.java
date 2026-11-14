package com.avicontrol.domain.exception.sacrificio.sanitario;

import com.avicontrol.domain.exception.DomainException;

public class DiagnosticoNoElegibleException extends DomainException {
  public DiagnosticoNoElegibleException() {
    super("El diagnóstico no requiere sacrificio sanitario");
  }
}
