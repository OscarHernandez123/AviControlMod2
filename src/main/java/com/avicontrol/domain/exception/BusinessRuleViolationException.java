package com.avicontrol.domain.exception;

public final class BusinessRuleViolationException extends DomainException {

  public BusinessRuleViolationException(String code, String message) {
    super(code, message);
  }
}
