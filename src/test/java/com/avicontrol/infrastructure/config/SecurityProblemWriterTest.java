package com.avicontrol.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class SecurityProblemWriterTest {
  private final ObjectMapper mapper = new ObjectMapper();
  private final SecurityProblemWriter writer = new SecurityProblemWriter(mapper);

  @Test
  void escribe401ProblemJsonConCorrelationIdYAuthenticateHeader() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/inventario");
    String correlationId = UUID.randomUUID().toString();
    request.setAttribute("correlationId", correlationId);
    MockHttpServletResponse response = new MockHttpServletResponse();

    writer.commence(request, response, new BadCredentialsException("Credenciales inválidas"));

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentType()).startsWith("application/problem+json");
    assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Basic realm=\"AviControl\"");
    assertThat(response.getHeader("X-Correlation-Id")).isEqualTo(correlationId);
    JsonNode body = mapper.readTree(response.getContentAsString());
    assertThat(body.get("code").asText()).isEqualTo("NO_AUTENTICADO");
    assertThat(body.get("status").asInt()).isEqualTo(401);
    assertThat(body.get("detail").asText()).isEqualTo("Autenticación requerida");
    assertThat(body.get("correlationId").asText()).isEqualTo(correlationId);
    assertThat(body.get("instance").asText()).isEqualTo("/api/inventario");
  }

  @Test
  void escribe403ProblemJsonConCorrelationIdGeneradoSiAusente() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/enfermedades");
    MockHttpServletResponse response = new MockHttpServletResponse();

    writer.handle(request, response, new AccessDeniedException("Acceso denegado"));

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getContentType()).startsWith("application/problem+json");
    assertThat(response.getHeader("WWW-Authenticate")).isNull();
    String headerCorr = response.getHeader("X-Correlation-Id");
    assertThat(headerCorr).isNotBlank();
    assertThat(UUID.fromString(headerCorr)).isNotNull();
    JsonNode body = mapper.readTree(response.getContentAsString());
    assertThat(body.get("code").asText()).isEqualTo("ACCESO_DENEGADO");
    assertThat(body.get("status").asInt()).isEqualTo(403);
    assertThat(body.get("detail").asText()).isEqualTo("Rol insuficiente");
    assertThat(body.get("correlationId").asText()).isEqualTo(headerCorr);
  }
}
