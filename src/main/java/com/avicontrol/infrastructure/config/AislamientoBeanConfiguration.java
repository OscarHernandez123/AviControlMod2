package com.avicontrol.infrastructure.config;

import com.avicontrol.application.sanitario.aislamiento.ValidarAislamientoUseCase;
import com.avicontrol.domain.port.out.modulo1.GalponQueryPort;
import com.avicontrol.domain.port.out.modulo1.GalponUpdatePort;
import com.avicontrol.domain.port.out.sanitario.aislamiento.SolicitudAislamientoRepositoryPort;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AislamientoBeanConfiguration {
  @Bean
  ValidarAislamientoUseCase validarAislamiento(
      SolicitudAislamientoRepositoryPort solicitudes,
      GalponQueryPort galpones,
      GalponUpdatePort actualizacion,
      Clock clock) {
    return new ValidarAislamientoUseCase(solicitudes, galpones, actualizacion, clock);
  }
}
