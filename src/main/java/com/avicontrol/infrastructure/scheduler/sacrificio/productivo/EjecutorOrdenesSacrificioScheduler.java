package com.avicontrol.infrastructure.scheduler.sacrificio.productivo;

import com.avicontrol.application.sacrificio.productivo.EjecutarOrdenesSacrificioVencidasUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EjecutorOrdenesSacrificioScheduler {
  private static final Logger log =
      LoggerFactory.getLogger(EjecutorOrdenesSacrificioScheduler.class);
  private final EjecutarOrdenesSacrificioVencidasUseCase ejecucion;

  public EjecutorOrdenesSacrificioScheduler(EjecutarOrdenesSacrificioVencidasUseCase ejecucion) {
    this.ejecucion = ejecucion;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void alIniciar() {
    ejecutar();
  }

  @Scheduled(
      fixedDelayString = "${avicontrol.sacrificio.productivo.intervalo-ms:30000}",
      initialDelayString = "${avicontrol.sacrificio.productivo.intervalo-ms:30000}")
  public void programado() {
    ejecutar();
  }

  private void ejecutar() {
    int completadas = ejecucion.ejecutar();
    if (completadas > 0) log.info("Órdenes de sacrificio ejecutadas: {}", completadas);
  }
}
