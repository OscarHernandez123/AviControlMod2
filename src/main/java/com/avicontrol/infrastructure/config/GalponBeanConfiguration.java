package com.avicontrol.infrastructure.config;

import com.avicontrol.application.galpon.ConsultarEdadPorGalponUseCase;
import com.avicontrol.application.galpon.ConsultarGalponUseCase;
import com.avicontrol.application.galpon.ConsultarListadoGalponesUseCase;
import com.avicontrol.application.galpon.ConsultarResumenGalponesUseCase;
import com.avicontrol.application.support.CurrentActorProvider;
import com.avicontrol.domain.port.out.galpon.GalponQueryPort;
import com.avicontrol.domain.port.out.galpon.LoteQueryPort;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class GalponBeanConfiguration {

  @Bean
  ConsultarEdadPorGalponUseCase consultarEdadPorGalponUseCase(
      GalponQueryPort galponQueryPort,
      LoteQueryPort loteQueryPort,
      CurrentActorProvider currentActorProvider,
      Clock businessClock) {
    return new ConsultarEdadPorGalponUseCase(
        galponQueryPort, loteQueryPort, currentActorProvider, businessClock);
  }

  @Bean
  ConsultarGalponUseCase consultarGalponUseCase(
      GalponQueryPort galponQueryPort,
      LoteQueryPort loteQueryPort,
      CurrentActorProvider currentActorProvider,
      Clock businessClock) {
    return new ConsultarGalponUseCase(
        galponQueryPort, loteQueryPort, currentActorProvider, businessClock);
  }

  @Bean
  ConsultarResumenGalponesUseCase consultarResumenGalponesUseCase(
      GalponQueryPort galponQueryPort, CurrentActorProvider currentActorProvider) {
    return new ConsultarResumenGalponesUseCase(galponQueryPort, currentActorProvider);
  }

  @Bean
  ConsultarListadoGalponesUseCase consultarListadoGalponesUseCase(
      GalponQueryPort galponQueryPort, CurrentActorProvider currentActorProvider) {
    return new ConsultarListadoGalponesUseCase(galponQueryPort, currentActorProvider);
  }
}
