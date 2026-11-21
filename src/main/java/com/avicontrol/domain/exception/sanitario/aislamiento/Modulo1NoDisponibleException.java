package com.avicontrol.domain.exception.sanitario.aislamiento;

import com.avicontrol.domain.exception.DomainException;

public class Modulo1NoDisponibleException extends DomainException {
  public Modulo1NoDisponibleException() {
    super("Módulo 1 no disponible");
  }
}
