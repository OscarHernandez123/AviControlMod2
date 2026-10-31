package com.avicontrol.infrastructure.adapter.in.rest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avicontrol.infrastructure.adapter.out.security.SpringSecurityCurrentActorProvider;
import com.avicontrol.infrastructure.config.BaseSupportConfiguration;
import com.avicontrol.infrastructure.config.SecurityConfiguration;
import com.avicontrol.infrastructure.config.TimeConfiguration;
import com.avicontrol.infrastructure.error.SecurityProblemResponder;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BasePingController.class)
@Import({
  BaseSupportConfiguration.class,
  CorrelationIdFilter.class,
  SecurityConfiguration.class,
  SecurityProblemResponder.class,
  SpringSecurityCurrentActorProvider.class,
  TimeConfiguration.class
})
class BasePingControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void rejectsAnUnauthenticatedRequest() throws Exception {
    mockMvc
        .perform(get("/api/base/ping"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("Content-Type", "application/problem+json"))
        .andExpect(jsonPath("$.code").value("AUTENTICACION_REQUERIDA"))
        .andExpect(jsonPath("$.correlationId").isNotEmpty());
  }

  @Test
  void rejectsAnAuthenticatedUserWithoutTheRequiredRole() throws Exception {
    mockMvc
        .perform(get("/api/base/ping").with(user("trabajador").roles("TRABAJADOR")))
        .andExpect(status().isForbidden())
        .andExpect(header().string("Content-Type", "application/problem+json"))
        .andExpect(jsonPath("$.code").value("ACCESO_NO_AUTORIZADO"));
  }

  @Test
  void returnsTheActorAndPropagatesTheCorrelationIdForAnAdministrator() throws Exception {
    String correlationId = UUID.randomUUID().toString();

    mockMvc
        .perform(
            get("/api/base/ping")
                .header(CorrelationIdFilter.HEADER_NAME, correlationId)
                .with(user("administrador").roles("ADMINISTRADOR")))
        .andExpect(status().isOk())
        .andExpect(header().string(CorrelationIdFilter.HEADER_NAME, correlationId))
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.actorId").value("administrador"))
        .andExpect(jsonPath("$.occurredAt").isNotEmpty());
  }
}
