package com.avicontrol.infrastructure.config;

import com.avicontrol.application.sacrificio.productivo.ConsultarResultadoSacrificioUseCase;
import com.avicontrol.application.sacrificio.productivo.CorregirResultadoSacrificioUseCase;
import com.avicontrol.application.sacrificio.productivo.MarcarResultadoUtilizadoUseCase;
import com.avicontrol.application.sacrificio.productivo.RegistrarResultadoSacrificioUseCase;
import com.avicontrol.domain.port.out.sacrificio.productivo.OrdenSacrificioProductivoQueryPort;
import com.avicontrol.domain.port.out.sacrificio.productivo.ResultadoSacrificioRepositoryPort;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResultadoSacrificioBeanConfiguration {
  @Bean
  RegistrarResultadoSacrificioUseCase registrarResultado(
      OrdenSacrificioProductivoQueryPort ordenes,
      ResultadoSacrificioRepositoryPort resultados,
      Clock clock) {
    return new RegistrarResultadoSacrificioUseCase(ordenes, resultados, clock);
  }

  @Bean
  CorregirResultadoSacrificioUseCase corregirResultado(
      OrdenSacrificioProductivoQueryPort ordenes,
      ResultadoSacrificioRepositoryPort resultados,
      Clock clock) {
    return new CorregirResultadoSacrificioUseCase(ordenes, resultados, clock);
  }

  @Bean
  ConsultarResultadoSacrificioUseCase consultarResultado(
      ResultadoSacrificioRepositoryPort resultados) {
    return new ConsultarResultadoSacrificioUseCase(resultados);
  }

  @Bean
  MarcarResultadoUtilizadoUseCase marcarResultadoUtilizado(
      ResultadoSacrificioRepositoryPort resultados) {
    return new MarcarResultadoUtilizadoUseCase(resultados);
  }
}
