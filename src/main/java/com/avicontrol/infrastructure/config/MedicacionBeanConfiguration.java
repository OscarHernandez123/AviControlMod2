package com.avicontrol.infrastructure.config;

import com.avicontrol.application.sanitario.medicacion.EditarMedicacionUseCase;
import com.avicontrol.application.sanitario.medicacion.RegistrarMedicacionUseCase;
import com.avicontrol.domain.port.out.sanitario.medicacion.EnfermedadQueryPort;
import com.avicontrol.domain.port.out.sanitario.medicacion.MedicacionRepositoryPort;
import com.avicontrol.domain.port.out.sanitario.medicacion.MedicamentoCatalogoQueryPort;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MedicacionBeanConfiguration {
  @Bean
  RegistrarMedicacionUseCase registrarMedicacion(
      MedicacionRepositoryPort repository,
      EnfermedadQueryPort enfermedades,
      MedicamentoCatalogoQueryPort medicamentos,
      Clock clock) {
    return new RegistrarMedicacionUseCase(repository, enfermedades, medicamentos, clock);
  }

  @Bean
  EditarMedicacionUseCase editarMedicacion(
      MedicacionRepositoryPort repository,
      EnfermedadQueryPort enfermedades,
      MedicamentoCatalogoQueryPort medicamentos,
      Clock clock) {
    return new EditarMedicacionUseCase(repository, enfermedades, medicamentos, clock);
  }
}
