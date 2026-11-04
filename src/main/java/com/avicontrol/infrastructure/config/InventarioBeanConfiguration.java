package com.avicontrol.infrastructure.config;

import com.avicontrol.application.inventario.AjustarEntradaRecepcionUseCase;
import com.avicontrol.application.inventario.RegistrarEntradaInventarioUseCase;
import com.avicontrol.application.inventario.VerificarRecepcionUtilizadaUseCase;
import com.avicontrol.application.support.CurrentActorProvider;
import com.avicontrol.domain.port.out.inventario.MovimientoAlimentoRepositoryPort;
import com.avicontrol.domain.port.out.inventario.MovimientoMedicamentoRepositoryPort;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class InventarioBeanConfiguration {
  @Bean
  RegistrarEntradaInventarioUseCase registrarEntradaInventarioUseCase(
      MovimientoAlimentoRepositoryPort a,
      MovimientoMedicamentoRepositoryPort m,
      CurrentActorProvider actor,
      Clock clock) {
    return new RegistrarEntradaInventarioUseCase(a, m, actor, clock);
  }

  @Bean
  AjustarEntradaRecepcionUseCase ajustarEntradaRecepcionUseCase(
      MovimientoAlimentoRepositoryPort a,
      MovimientoMedicamentoRepositoryPort m,
      CurrentActorProvider actor,
      Clock clock) {
    return new AjustarEntradaRecepcionUseCase(a, m, actor, clock);
  }

  @Bean
  VerificarRecepcionUtilizadaUseCase verificarRecepcionUtilizadaUseCase(
      MovimientoAlimentoRepositoryPort a, MovimientoMedicamentoRepositoryPort m) {
    return new VerificarRecepcionUtilizadaUseCase(a, m);
  }
}
