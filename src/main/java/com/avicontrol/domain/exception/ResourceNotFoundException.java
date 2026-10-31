package com.avicontrol.domain.exception;

public class ResourceNotFoundException extends DomainException {

  public ResourceNotFoundException(String message) {
    super("RECURSO_NO_ENCONTRADO", message);
  }
}
