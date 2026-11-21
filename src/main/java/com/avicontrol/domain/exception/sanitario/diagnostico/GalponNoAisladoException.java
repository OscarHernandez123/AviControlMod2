package com.avicontrol.domain.exception.sanitario.diagnostico;

import com.avicontrol.domain.exception.DomainException;

public class GalponNoAisladoException extends DomainException {
  public GalponNoAisladoException() {
    super("El galpón debe estar en aislamiento");
  }
}
