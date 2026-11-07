package com.avicontrol.infrastructure.config;

import com.avicontrol.application.medicamento.RegistrarConsumoMedicamentoUseCase;
import com.avicontrol.domain.port.out.medicamento.ConsumoMedicamentoRepositoryPort;
import com.avicontrol.domain.port.out.medicamento.DiagnosticoQueryPort;
import com.avicontrol.domain.port.out.medicamento.InventarioMedicamentoPort;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConsumoMedicamentoBeanConfiguration {
  @Bean
  RegistrarConsumoMedicamentoUseCase registrarConsumoMedicamento(
      ConsumoMedicamentoRepositoryPort consumos,
      DiagnosticoQueryPort diagnosticos,
      InventarioMedicamentoPort inventario,
      Clock clock) {
    return new RegistrarConsumoMedicamentoUseCase(consumos, diagnosticos, inventario, clock);
  }
}
