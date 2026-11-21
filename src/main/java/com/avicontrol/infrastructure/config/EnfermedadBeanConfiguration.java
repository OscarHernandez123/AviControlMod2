package com.avicontrol.infrastructure.config;

import com.avicontrol.application.sanitario.enfermedad.EditarEnfermedadUseCase;
import com.avicontrol.application.sanitario.enfermedad.ListarEnfermedadesUseCase;
import com.avicontrol.application.sanitario.enfermedad.RegistrarEnfermedadUseCase;
import com.avicontrol.domain.port.out.sanitario.enfermedad.EnfermedadRepositoryPort;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EnfermedadBeanConfiguration {
  @Bean
  ListarEnfermedadesUseCase listarEnfermedades(EnfermedadRepositoryPort repository) {
    return new ListarEnfermedadesUseCase(repository);
  }

  @Bean
  RegistrarEnfermedadUseCase registrarEnfermedad(EnfermedadRepositoryPort repository, Clock clock) {
    return new RegistrarEnfermedadUseCase(repository, clock);
  }

  @Bean
  EditarEnfermedadUseCase editarEnfermedad(EnfermedadRepositoryPort repository, Clock clock) {
    return new EditarEnfermedadUseCase(repository, clock);
  }
}
