package com.avicontrol.infrastructure.security;

import com.avicontrol.infrastructure.adapter.in.rest.shared.ProblemJsonWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@org.springframework.context.annotation.Profile({"dev", "test"})
public class MockRoleFilter extends OncePerRequestFilter {

  public static final UUID DEFAULT_ACTOR_ID =
      UUID.fromString("a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d");

  private static final Set<String> ALLOWED_ROLES =
      Set.of("ROLE_VETERINARIO", "ROLE_TRABAJADOR", "ROLE_ADMINISTRADOR");

  private final ProblemJsonWriter problemJsonWriter;

  public MockRoleFilter(ProblemJsonWriter problemJsonWriter) {
    this.problemJsonWriter = problemJsonWriter;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String role = request.getHeader("X-Mock-Role");
    if (role == null || role.isBlank()) {
      role = "ROLE_VETERINARIO";
    }
    if (!ALLOWED_ROLES.contains(role)) {
      problemJsonWriter.write(
          request,
          response,
          HttpStatus.FORBIDDEN,
          "ROL_MOCK_NO_PERMITIDO",
          "El rol indicado no está permitido en el perfil de desarrollo.");
      return;
    }

    UUID actorId;
    try {
      String actorIdHeader = request.getHeader("X-Mock-Actor-Id");
      actorId =
          actorIdHeader == null || actorIdHeader.isBlank()
              ? DEFAULT_ACTOR_ID
              : UUID.fromString(actorIdHeader);
    } catch (IllegalArgumentException exception) {
      problemJsonWriter.write(
          request,
          response,
          HttpStatus.FORBIDDEN,
          "ACTOR_MOCK_INVALIDO",
          "X-Mock-Actor-Id debe contener un UUID válido.");
      return;
    }

    MockActor actor = new MockActor(actorId, role);
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(
            actor, null, List.of(new SimpleGrantedAuthority(role)));
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    try {
      filterChain.doFilter(request, response);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }
}
