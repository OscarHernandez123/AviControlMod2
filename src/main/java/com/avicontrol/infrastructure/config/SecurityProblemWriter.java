package com.avicontrol.infrastructure.config;

import com.avicontrol.infrastructure.adapter.in.rest.error.ApiErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class SecurityProblemWriter implements AuthenticationEntryPoint, AccessDeniedHandler {
  private final ObjectMapper mapper;

  public SecurityProblemWriter(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws IOException, ServletException {
    escribir(
        request, response, HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO", "Autenticación requerida");
  }

  @Override
  public void handle(
      HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
      throws IOException, ServletException {
    escribir(request, response, HttpStatus.FORBIDDEN, "ACCESO_DENEGADO", "Rol insuficiente");
  }

  private void escribir(
      HttpServletRequest request,
      HttpServletResponse response,
      HttpStatus status,
      String code,
      String detail)
      throws IOException {
    String correlationId = (String) request.getAttribute("correlationId");
    if (correlationId == null) correlationId = UUID.randomUUID().toString();
    ApiErrorResponse body =
        new ApiErrorResponse(
            URI.create("https://avicontrol/errors/" + code.toLowerCase().replace('_', '-')),
            status.getReasonPhrase(),
            status.value(),
            detail,
            request.getRequestURI(),
            code,
            correlationId,
            List.of());
    response.setStatus(status.value());
    if (status == HttpStatus.UNAUTHORIZED) {
      response.setHeader("WWW-Authenticate", "Basic realm=\"AviControl\"");
    }
    response.setContentType("application/problem+json");
    response.setCharacterEncoding("UTF-8");
    response.setHeader("X-Correlation-Id", correlationId);
    response.getWriter().write(mapper.writeValueAsString(body));
  }
}
