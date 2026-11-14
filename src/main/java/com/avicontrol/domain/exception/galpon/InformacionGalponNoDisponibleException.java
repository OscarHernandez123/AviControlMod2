package com.avicontrol.domain.exception.galpon;

import com.avicontrol.domain.exception.DomainException;

public class InformacionGalponNoDisponibleException extends DomainException {
  public InformacionGalponNoDisponibleException() {
    super("La información vigente del Módulo 1 no está disponible");
  }
}
