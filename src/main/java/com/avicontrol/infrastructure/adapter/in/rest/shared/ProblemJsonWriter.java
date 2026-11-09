package com.avicontrol.infrastructure.adapter.in.rest.shared;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class ProblemJsonWriter {

  private final ObjectMapper objectMapper;
  private final ApiProblemFactory problemFactory;

  public ProblemJsonWriter(ObjectMapper objectMapper, ApiProblemFactory problemFactory) {
    this.objectMapper = objectMapper;
    this.problemFactory = problemFactory;
  }

  public void write(
      HttpServletRequest request,
      HttpServletResponse response,
      HttpStatus status,
      String code,
      String detail)
      throws IOException {
    ApiProblem problem = problemFactory.create(request, status, code, detail);
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setHeader("X-Correlation-Id", problem.correlationId());
    objectMapper.writeValue(response.getOutputStream(), problem);
  }
}
