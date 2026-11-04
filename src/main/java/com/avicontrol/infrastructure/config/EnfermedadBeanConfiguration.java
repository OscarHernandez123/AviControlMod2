package com.avicontrol.infrastructure.config;

import com.avicontrol.application.sanitario.enfermedad.EditarEnfermedadUseCase;
import com.avicontrol.application.sanitario.enfermedad.RegistrarEnfermedadUseCase;
import com.avicontrol.application.support.CurrentActorProvider;
import com.avicontrol.domain.port.out.sanitario.EnfermedadRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class EnfermedadBeanConfiguration {
  @Bean
  RegistrarEnfermedadUseCase registrarEnfermedadUseCase(
      EnfermedadRepositoryPort repository, CurrentActorProvider actor) {
    return new RegistrarEnfermedadUseCase(repository, actor);
  }

  @Bean
  EditarEnfermedadUseCase editarEnfermedadUseCase(
      EnfermedadRepositoryPort repository, CurrentActorProvider actor) {
    return new EditarEnfermedadUseCase(repository, actor);
  }
}
