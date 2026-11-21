package com.avicontrol.domain.exception.medicamento;

import com.avicontrol.domain.exception.DomainException;

public class IdempotenciaConsumoIncompatibleException extends DomainException {
  public IdempotenciaConsumoIncompatibleException() {
    super("La clave de idempotencia ya se usó con otros datos");
  }
}
