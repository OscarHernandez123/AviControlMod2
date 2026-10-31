package com.avicontrol.infrastructure.adapter.out.security;

import com.avicontrol.application.support.CurrentActor;
import com.avicontrol.application.support.CurrentActorProvider;
import java.util.Set;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SpringSecurityCurrentActorProvider implements CurrentActorProvider {

  @Override
  public CurrentActor currentActor() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new AuthenticationCredentialsNotFoundException("No hay un actor autenticado");
    }
    Set<String> authorities =
        authentication.getAuthorities().stream()
            .map(authority -> authority.getAuthority())
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    return new CurrentActor(authentication.getName(), authorities);
  }
}
