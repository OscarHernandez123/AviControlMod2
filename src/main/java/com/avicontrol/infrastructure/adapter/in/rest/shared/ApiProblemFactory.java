package com.avicontrol.infrastructure.adapter.in.rest.shared;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ApiProblemFactory {

  public ApiProblem create(
      HttpServletRequest request, HttpStatus status, String code, String detail) {
    String correlationId = UUID.randomUUID().toString();
    request.setAttribute("correlationId", correlationId);
    return new ApiProblem(
        "https://avicontrol/errors/" + code.toLowerCase(),
        status.getReasonPhrase(),
        status.value(),
        detail,
        request.getRequestURI(),
        code,
        correlationId,
        List.of());
  }
}
