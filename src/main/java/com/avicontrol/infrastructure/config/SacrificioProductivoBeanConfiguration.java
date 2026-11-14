package com.avicontrol.infrastructure.config;

import com.avicontrol.application.sacrificio.productivo.CancelarOrdenSacrificioUseCase;
import com.avicontrol.application.sacrificio.productivo.ConsultarOrdenesSacrificioUseCase;
import com.avicontrol.application.sacrificio.productivo.ConsultarResumenOrdenesSacrificioUseCase;
import com.avicontrol.application.sacrificio.productivo.EjecutarOrdenSacrificioUseCase;
import com.avicontrol.application.sacrificio.productivo.EjecutarOrdenesSacrificioVencidasUseCase;
import com.avicontrol.application.sacrificio.productivo.ProgramarOrdenSacrificioUseCase;
import com.avicontrol.application.sacrificio.productivo.ReprogramarOrdenSacrificioUseCase;
import com.avicontrol.application.sacrificio.productivo.ValidarAptitudSacrificioUseCase;
import com.avicontrol.domain.port.out.modulo1.GalponQueryPort;
import com.avicontrol.domain.port.out.modulo1.GalponUpdatePort;
import com.avicontrol.domain.port.out.modulo1.LoteQueryPort;
import com.avicontrol.domain.port.out.modulo1.LoteUpdatePort;
import com.avicontrol.domain.port.out.sacrificio.productivo.OrdenSacrificioProductivoRepositoryPort;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SacrificioProductivoBeanConfiguration {
  @Bean
  ValidarAptitudSacrificioUseCase validarAptitudSacrificio(
      GalponQueryPort galpones, LoteQueryPort lotes, GalponUpdatePort cambios, Clock clock) {
    return new ValidarAptitudSacrificioUseCase(galpones, lotes, cambios, clock);
  }

  @Bean
  ProgramarOrdenSacrificioUseCase programarOrden(
      GalponQueryPort galpones,
      LoteQueryPort lotes,
      OrdenSacrificioProductivoRepositoryPort ordenes,
      Clock clock) {
    return new ProgramarOrdenSacrificioUseCase(galpones, lotes, ordenes, clock);
  }

  @Bean
  ReprogramarOrdenSacrificioUseCase reprogramarOrden(
      OrdenSacrificioProductivoRepositoryPort ordenes,
      GalponQueryPort galpones,
      LoteQueryPort lotes,
      Clock clock) {
    return new ReprogramarOrdenSacrificioUseCase(ordenes, galpones, lotes, clock);
  }

  @Bean
  CancelarOrdenSacrificioUseCase cancelarOrden(
      OrdenSacrificioProductivoRepositoryPort ordenes, Clock clock) {
    return new CancelarOrdenSacrificioUseCase(ordenes, clock);
  }

  @Bean
  EjecutarOrdenSacrificioUseCase ejecutarOrden(
      OrdenSacrificioProductivoRepositoryPort ordenes,
      GalponQueryPort galpones,
      LoteQueryPort lotes,
      GalponUpdatePort cambiosGalpon,
      LoteUpdatePort cambiosLote,
      Clock clock) {
    return new EjecutarOrdenSacrificioUseCase(
        ordenes, galpones, lotes, cambiosGalpon, cambiosLote, clock);
  }

  @Bean
  EjecutarOrdenesSacrificioVencidasUseCase ejecutarVencidas(
      OrdenSacrificioProductivoRepositoryPort ordenes,
      EjecutarOrdenSacrificioUseCase ejecucion,
      Clock clock) {
    return new EjecutarOrdenesSacrificioVencidasUseCase(ordenes, ejecucion, clock);
  }

  @Bean
  ConsultarResumenOrdenesSacrificioUseCase resumenOrdenes(
      OrdenSacrificioProductivoRepositoryPort ordenes) {
    return new ConsultarResumenOrdenesSacrificioUseCase(ordenes);
  }

  @Bean
  ConsultarOrdenesSacrificioUseCase consultarOrdenes(
      OrdenSacrificioProductivoRepositoryPort ordenes) {
    return new ConsultarOrdenesSacrificioUseCase(ordenes);
  }
}
