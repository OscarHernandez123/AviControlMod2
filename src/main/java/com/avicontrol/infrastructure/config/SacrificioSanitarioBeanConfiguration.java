package com.avicontrol.infrastructure.config;

import com.avicontrol.application.sacrificio.sanitario.ConfirmarEjecucionSacrificioSanitarioUseCase;
import com.avicontrol.application.sacrificio.sanitario.EmitirOrdenSacrificioSanitarioUseCase;
import com.avicontrol.domain.port.out.modulo1.GalponUpdatePort;
import com.avicontrol.domain.port.out.modulo1.LoteQueryPort;
import com.avicontrol.domain.port.out.modulo1.LoteUpdatePort;
import com.avicontrol.domain.port.out.sacrificio.sanitario.DiagnosticoSanitarioQueryPort;
import com.avicontrol.domain.port.out.sacrificio.sanitario.OrdenSacrificioSanitarioRepositoryPort;
import com.avicontrol.domain.port.out.sanitario.aislamiento.GalponLoteAislamientoQueryPort;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SacrificioSanitarioBeanConfiguration {
  @Bean
  EmitirOrdenSacrificioSanitarioUseCase emitirOrdenSanitaria(
      DiagnosticoSanitarioQueryPort diagnosticos,
      GalponLoteAislamientoQueryPort galpones,
      LoteQueryPort lotes,
      OrdenSacrificioSanitarioRepositoryPort ordenes,
      Clock clock) {
    return new EmitirOrdenSacrificioSanitarioUseCase(diagnosticos, galpones, lotes, ordenes, clock);
  }

  @Bean
  ConfirmarEjecucionSacrificioSanitarioUseCase confirmarOrdenSanitaria(
      OrdenSacrificioSanitarioRepositoryPort ordenes,
      GalponLoteAislamientoQueryPort galpones,
      LoteQueryPort lotes,
      GalponUpdatePort cambioGalpon,
      LoteUpdatePort cambioLote,
      Clock clock) {
    return new ConfirmarEjecucionSacrificioSanitarioUseCase(
        ordenes, galpones, lotes, cambioGalpon, cambioLote, clock);
  }
}
