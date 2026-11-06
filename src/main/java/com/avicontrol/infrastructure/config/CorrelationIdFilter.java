package com.avicontrol.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String supplied = request.getHeader("X-Correlation-Id");
    String id;
    try {
      id = UUID.fromString(supplied).toString();
    } catch (IllegalArgumentException | NullPointerException exception) {
      id = UUID.randomUUID().toString();
    }
    request.setAttribute("correlationId", id);
    response.setHeader("X-Correlation-Id", id);
    try (MDC.MDCCloseable ignored = MDC.putCloseable("correlationId", id)) {
      chain.doFilter(request, response);
    }
  }
}
