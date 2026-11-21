package com.avicontrol.domain.exception.medicamento;

import com.avicontrol.domain.exception.DomainException;

public class DiagnosticoConsumoNoEncontradoException extends DomainException {
  public DiagnosticoConsumoNoEncontradoException() {
    super("Diagnóstico no encontrado");
  }
}
