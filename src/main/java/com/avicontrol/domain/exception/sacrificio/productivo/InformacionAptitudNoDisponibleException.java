package com.avicontrol.domain.exception.sacrificio.productivo;

import com.avicontrol.domain.exception.DomainException;

public class InformacionAptitudNoDisponibleException extends DomainException {
  public InformacionAptitudNoDisponibleException() {
    super("La información vigente para validar la aptitud está incompleta");
  }
}
