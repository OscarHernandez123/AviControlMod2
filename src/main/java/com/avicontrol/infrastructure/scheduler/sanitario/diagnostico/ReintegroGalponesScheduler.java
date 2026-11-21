package com.avicontrol.infrastructure.scheduler.sanitario.diagnostico;

import com.avicontrol.application.sanitario.diagnostico.ReintegrarGalponesTratadosUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReintegroGalponesScheduler {
  private static final Logger log = LoggerFactory.getLogger(ReintegroGalponesScheduler.class);
  private final ReintegrarGalponesTratadosUseCase reintegro;

  public ReintegroGalponesScheduler(ReintegrarGalponesTratadosUseCase reintegro) {
    this.reintegro = reintegro;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void alIniciar() {
    ejecutar();
  }

  @Scheduled(
      fixedDelayString = "${avicontrol.diagnostico.reintegro.intervalo-ms:60000}",
      initialDelayString = "${avicontrol.diagnostico.reintegro.intervalo-ms:60000}")
  public void programado() {
    ejecutar();
  }

  private void ejecutar() {
    int completados = reintegro.ejecutar();
    if (completados > 0) log.info("Galpones reintegrados: {}", completados);
  }
}
