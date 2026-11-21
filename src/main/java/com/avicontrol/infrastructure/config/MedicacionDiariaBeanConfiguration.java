package com.avicontrol.infrastructure.config;

import com.avicontrol.application.sanitario.medicacion.ConsultarMedicacionDiariaGalponUseCase;
import com.avicontrol.application.sanitario.medicacion.ConsultarResumenMedicacionDiariaUseCase;
import com.avicontrol.domain.port.out.sanitario.medicacion.DiagnosticoTratamientoQueryPort;
import com.avicontrol.domain.port.out.sanitario.medicacion.GalponLoteQueryPort;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MedicacionDiariaBeanConfiguration {
  @Bean
  ConsultarMedicacionDiariaGalponUseCase consultarMedicacionDiariaGalpon(
      GalponLoteQueryPort galpones, DiagnosticoTratamientoQueryPort diagnosticos, Clock clock) {
    return new ConsultarMedicacionDiariaGalponUseCase(galpones, diagnosticos, clock);
  }

  @Bean
  ConsultarResumenMedicacionDiariaUseCase consultarResumenMedicacionDiaria(
      ConsultarMedicacionDiariaGalponUseCase detalle, Clock clock) {
    return new ConsultarResumenMedicacionDiariaUseCase(detalle, clock);
  }
}
