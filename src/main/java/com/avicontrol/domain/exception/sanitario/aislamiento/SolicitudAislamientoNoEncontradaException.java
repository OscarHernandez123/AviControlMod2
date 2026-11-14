package com.avicontrol.domain.exception.sanitario.aislamiento;

import com.avicontrol.domain.exception.DomainException;

public class SolicitudAislamientoNoEncontradaException extends DomainException {
  public SolicitudAislamientoNoEncontradaException() {
    super("Solicitud de aislamiento no encontrada");
  }
}
