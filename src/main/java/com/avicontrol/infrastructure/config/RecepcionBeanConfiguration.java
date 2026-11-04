package com.avicontrol.infrastructure.config;

import com.avicontrol.application.catalogo.port.AlimentoCatalogoQueryPort;
import com.avicontrol.application.catalogo.port.MedicamentoCatalogoQueryPort;
import com.avicontrol.application.recepcion.GestionarRecepcionesUseCase;
import com.avicontrol.application.support.CurrentActorProvider;
import com.avicontrol.domain.port.out.recepcion.AuditoriaRecepcionRepositoryPort;
import com.avicontrol.domain.port.out.recepcion.InventarioRecepcionPort;
import com.avicontrol.domain.port.out.recepcion.RecepcionAlimentoRepositoryPort;
import com.avicontrol.domain.port.out.recepcion.RecepcionEventPublisherPort;
import com.avicontrol.domain.port.out.recepcion.RecepcionMedicamentoRepositoryPort;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class RecepcionBeanConfiguration {
  @Bean
  GestionarRecepcionesUseCase gestionarRecepcionesUseCase(
      RecepcionAlimentoRepositoryPort alimentos,
      RecepcionMedicamentoRepositoryPort medicamentos,
      AlimentoCatalogoQueryPort catalogoAlimentos,
      MedicamentoCatalogoQueryPort catalogoMedicamentos,
      InventarioRecepcionPort inventario,
      AuditoriaRecepcionRepositoryPort auditoria,
      CurrentActorProvider actor,
      Clock clock,
      RecepcionEventPublisherPort events) {
    return new GestionarRecepcionesUseCase(
        alimentos,
        medicamentos,
        catalogoAlimentos,
        catalogoMedicamentos,
        inventario,
        auditoria,
        actor,
        clock,
        events);
  }
}
