package com.avicontrol.domain.exception;

public class BusinessRuleViolationException extends DomainException {

  public BusinessRuleViolationException(String code, String message) {
    super(code, message);
  }
}
