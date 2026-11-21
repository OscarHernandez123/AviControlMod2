package com.avicontrol.domain.exception.sanitario.aislamiento;

import com.avicontrol.domain.exception.DomainException;

public class SolicitudAislamientoNoPendienteException extends DomainException {
  public SolicitudAislamientoNoPendienteException() {
    super("La solicitud ya fue resuelta");
  }
}
