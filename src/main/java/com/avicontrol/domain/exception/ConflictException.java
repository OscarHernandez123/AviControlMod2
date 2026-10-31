package com.avicontrol.domain.exception;

public class ConflictException extends DomainException {

  public ConflictException(String message) {
    super("CONFLICTO_DE_ESTADO", message);
  }
}
