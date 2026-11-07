package com.avicontrol.infrastructure.config;

import com.avicontrol.application.alerta.ConsultarAlertasSanitariasUseCase;
import com.avicontrol.application.alerta.GenerarAlertaSanitariaUseCase;
import com.avicontrol.application.modulo1.ActualizarInventarioVivoUseCase;
import com.avicontrol.application.mortalidad.RegistrarMortalidadAnormalUseCase;
import com.avicontrol.application.mortalidad.RegistrarMortalidadUseCase;
import com.avicontrol.domain.port.out.alerta.AlertaSanitariaQueryPort;
import com.avicontrol.domain.port.out.alerta.AlertaSanitariaRepositoryPort;
import com.avicontrol.domain.port.out.modulo1.GalponQueryPort;
import com.avicontrol.domain.port.out.modulo1.LoteQueryPort;
import com.avicontrol.domain.port.out.mortalidad.RegistroMortalidadAnormalRepositoryPort;
import com.avicontrol.domain.port.out.mortalidad.RegistroMortalidadRepositoryPort;
import com.avicontrol.domain.port.out.sanitario.aislamiento.GalponLoteAislamientoQueryPort;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MortalidadBeanConfiguration {
  @Bean
  GenerarAlertaSanitariaUseCase generarAlertaSanitaria(
      AlertaSanitariaRepositoryPort repo, Clock clock) {
    return new GenerarAlertaSanitariaUseCase(repo, clock);
  }

  @Bean
  RegistrarMortalidadAnormalUseCase registrarMortalidadAnormal(
      GalponLoteAislamientoQueryPort galpones,
      LoteQueryPort lotes,
      RegistroMortalidadAnormalRepositoryPort reportes,
      GenerarAlertaSanitariaUseCase alertas,
      Clock clock) {
    return new RegistrarMortalidadAnormalUseCase(galpones, lotes, reportes, alertas, clock);
  }

  @Bean
  RegistrarMortalidadUseCase registrarMortalidad(
      GalponLoteAislamientoQueryPort galpones,
      LoteQueryPort lotes,
      ActualizarInventarioVivoUseCase inventario,
      RegistroMortalidadRepositoryPort registros,
      RegistrarMortalidadAnormalUseCase anormal,
      Clock clock) {
    return new RegistrarMortalidadUseCase(galpones, lotes, inventario, registros, anormal, clock);
  }

  @Bean
  ConsultarAlertasSanitariasUseCase consultarAlertasSanitarias(
      AlertaSanitariaQueryPort alertas,
      GalponQueryPort galpones,
      LoteQueryPort lotes,
      Clock clock) {
    return new ConsultarAlertasSanitariasUseCase(alertas, galpones, lotes, clock);
  }
}
