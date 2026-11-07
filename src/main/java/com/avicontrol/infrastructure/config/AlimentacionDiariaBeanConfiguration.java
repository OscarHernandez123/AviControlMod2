package com.avicontrol.infrastructure.config;

import com.avicontrol.application.alimentacion.ConsultarAlimentoGalponDiaUseCase;
import com.avicontrol.application.alimentacion.ConsultarResumenAlimentoDiarioUseCase;
import com.avicontrol.application.alimentacion.RegistrarSuministroDiarioUseCase;
import com.avicontrol.domain.port.out.alimentacion.InventarioAlimentoPort;
import com.avicontrol.domain.port.out.alimentacion.ObservabilidadAlimentacionPort;
import com.avicontrol.domain.port.out.alimentacion.PlanNutricionalQueryPort;
import com.avicontrol.domain.port.out.alimentacion.SuministroDiarioQueryPort;
import com.avicontrol.domain.port.out.alimentacion.SuministroDiarioRepositoryPort;
import com.avicontrol.domain.port.out.sanitario.medicacion.GalponLoteQueryPort;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AlimentacionDiariaBeanConfiguration {
  @Bean
  ConsultarAlimentoGalponDiaUseCase consultarAlimentoGalponDia(
      GalponLoteQueryPort galpones,
      PlanNutricionalQueryPort planes,
      SuministroDiarioQueryPort suministros,
      InventarioAlimentoPort inventario,
      ObservabilidadAlimentacionPort observabilidad,
      Clock clock) {
    return new ConsultarAlimentoGalponDiaUseCase(
        galpones, planes, suministros, inventario, observabilidad, clock);
  }

  @Bean
  ConsultarResumenAlimentoDiarioUseCase consultarResumenAlimentoDiario(
      ConsultarAlimentoGalponDiaUseCase detalle, Clock clock) {
    return new ConsultarResumenAlimentoDiarioUseCase(detalle, clock);
  }

  @Bean
  RegistrarSuministroDiarioUseCase registrarSuministroDiario(
      GalponLoteQueryPort galpones,
      PlanNutricionalQueryPort planes,
      InventarioAlimentoPort inventario,
      SuministroDiarioRepositoryPort suministros,
      Clock clock) {
    return new RegistrarSuministroDiarioUseCase(galpones, planes, inventario, suministros, clock);
  }
}
