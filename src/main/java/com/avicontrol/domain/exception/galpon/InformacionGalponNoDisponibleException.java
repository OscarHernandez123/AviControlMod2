package com.avicontrol.domain.exception.galpon;

import com.avicontrol.domain.exception.DomainException;

public final class InformacionGalponNoDisponibleException extends DomainException {

  public InformacionGalponNoDisponibleException() {
    super("INFORMACION_GALPON_NO_DISPONIBLE", "La información del galpón no está disponible");
  }
}
