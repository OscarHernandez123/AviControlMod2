package com.avicontrol.infrastructure.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.avicontrol.domain.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void returnsAnRfc9457CompatibleProblemWithTheCorrelationId() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/recurso/123");
    request.setAttribute("correlationId", "b952e2bc-0529-4309-9a2a-7eff6eaf87d0");

    ResponseEntity<?> response =
        handler.handleNotFound(
            new ResourceNotFoundException("No existe el recurso solicitado"), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getHeaders().getContentType())
        .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(response.getBody()).isInstanceOf(org.springframework.http.ProblemDetail.class);
    org.springframework.http.ProblemDetail problem =
        (org.springframework.http.ProblemDetail) response.getBody();
    assertThat(problem.getProperties())
        .containsEntry("code", "RECURSO_NO_ENCONTRADO")
        .containsEntry("correlationId", "b952e2bc-0529-4309-9a2a-7eff6eaf87d0");
  }
}
