package com.avicontrol.domain.exception.sanitario.aislamiento;

import com.avicontrol.domain.exception.DomainException;

public class SolicitudAislamientoDuplicadaException extends DomainException {
  public SolicitudAislamientoDuplicadaException() {
    super("Ya existe una solicitud pendiente para el galpón");
  }
}
