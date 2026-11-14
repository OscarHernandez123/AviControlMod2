package com.avicontrol.infrastructure.config;

import com.avicontrol.application.galpon.ConsultarEdadPorGalponUseCase;
import com.avicontrol.application.galpon.ConsultarGalponUseCase;
import com.avicontrol.application.galpon.ConsultarListadoGalponesUseCase;
import com.avicontrol.application.galpon.ConsultarResumenGalponesUseCase;
import com.avicontrol.domain.port.out.modulo1.GalponQueryPort;
import com.avicontrol.domain.port.out.modulo1.LoteQueryPort;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GalponConsultaBeanConfiguration {
  @Bean
  ConsultarEdadPorGalponUseCase consultarEdadPorGalpon(
      GalponQueryPort galpones, LoteQueryPort lotes, Clock clock) {
    return new ConsultarEdadPorGalponUseCase(galpones, lotes, clock);
  }

  @Bean
  ConsultarGalponUseCase consultarGalpon(
      GalponQueryPort galpones, LoteQueryPort lotes, Clock clock) {
    return new ConsultarGalponUseCase(galpones, lotes, clock);
  }

  @Bean
  ConsultarListadoGalponesUseCase consultarListadoGalpones(GalponQueryPort galpones) {
    return new ConsultarListadoGalponesUseCase(galpones);
  }

  @Bean
  ConsultarResumenGalponesUseCase consultarResumenGalpones(GalponQueryPort galpones) {
    return new ConsultarResumenGalponesUseCase(galpones);
  }
}
