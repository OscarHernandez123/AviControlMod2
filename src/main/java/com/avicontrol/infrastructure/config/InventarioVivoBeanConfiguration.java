package com.avicontrol.infrastructure.config;

import com.avicontrol.application.modulo1.ActualizarInventarioVivoUseCase;
import com.avicontrol.domain.port.out.modulo1.ActualizarInventarioVivoPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InventarioVivoBeanConfiguration {
  @Bean
  ActualizarInventarioVivoUseCase actualizarInventarioVivo(ActualizarInventarioVivoPort port) {
    return new ActualizarInventarioVivoUseCase(port);
  }
}
