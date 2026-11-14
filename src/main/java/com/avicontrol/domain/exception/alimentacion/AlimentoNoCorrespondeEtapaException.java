package com.avicontrol.domain.exception.alimentacion;

import com.avicontrol.domain.exception.DomainException;

public class AlimentoNoCorrespondeEtapaException extends DomainException {
  public AlimentoNoCorrespondeEtapaException() {
    super("El alimento no corresponde a la etapa nutricional vigente");
  }
}
