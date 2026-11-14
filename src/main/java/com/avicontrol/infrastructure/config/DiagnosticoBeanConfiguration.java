package com.avicontrol.infrastructure.config;

import com.avicontrol.application.sanitario.diagnostico.RegistrarDiagnosticoUseCase;
import com.avicontrol.application.sanitario.diagnostico.ReintegrarGalponesTratadosUseCase;
import com.avicontrol.domain.port.out.sanitario.aislamiento.GalponLoteAislamientoQueryPort;
import com.avicontrol.domain.port.out.sanitario.diagnostico.DiagnosticoGalponRepositoryPort;
import com.avicontrol.domain.port.out.sanitario.diagnostico.EnfermedadQueryPort;
import com.avicontrol.domain.port.out.sanitario.diagnostico.GalponReintegroPort;
import com.avicontrol.domain.port.out.sanitario.diagnostico.MedicacionQueryPort;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class DiagnosticoBeanConfiguration {
  @Bean
  RegistrarDiagnosticoUseCase registrarDiagnostico(
      DiagnosticoGalponRepositoryPort diagnosticos,
      EnfermedadQueryPort enfermedades,
      MedicacionQueryPort medicaciones,
      GalponLoteAislamientoQueryPort galpones,
      Clock clock) {
    return new RegistrarDiagnosticoUseCase(
        diagnosticos, enfermedades, medicaciones, galpones, clock);
  }

  @Bean
  ReintegrarGalponesTratadosUseCase reintegrarGalponesTratados(
      DiagnosticoGalponRepositoryPort diagnosticos,
      GalponLoteAislamientoQueryPort galpones,
      GalponReintegroPort reintegro,
      Clock clock,
      PlatformTransactionManager manager) {
    return new ReintegrarGalponesTratadosUseCase(
        diagnosticos, galpones, reintegro, clock, new TransactionTemplate(manager));
  }
}
