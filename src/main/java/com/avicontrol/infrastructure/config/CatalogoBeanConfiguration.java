package com.avicontrol.infrastructure.config;

import com.avicontrol.application.catalogo.GestionarCatalogoUseCase;
import com.avicontrol.application.support.CurrentActorProvider;
import com.avicontrol.domain.port.out.catalogo.AlimentoRepositoryPort;
import com.avicontrol.domain.port.out.catalogo.MedicamentoRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class CatalogoBeanConfiguration {
  @Bean
  GestionarCatalogoUseCase gestionarCatalogoUseCase(
      AlimentoRepositoryPort a, MedicamentoRepositoryPort m, CurrentActorProvider actor) {
    return new GestionarCatalogoUseCase(a, m, actor);
  }
}
