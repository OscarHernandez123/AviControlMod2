package com.avicontrol.infrastructure.adapter.in.event.sacrificio.productivo;

import com.avicontrol.application.sacrificio.productivo.MarcarResultadoUtilizadoUseCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class Modulo3LiquidacionLoteEventListener {
  private final MarcarResultadoUtilizadoUseCase caso;
  private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

  public Modulo3LiquidacionLoteEventListener(MarcarResultadoUtilizadoUseCase caso) {
    this.caso = caso;
  }

  @EventListener
  public void procesar(ResultadoSacrificioUtilizadoV1 evento) {
    if (evento == null
        || evento.eventId() == null
        || !"ResultadoSacrificioUtilizado".equals(evento.eventType())
        || evento.eventVersion() != 1
        || evento.payload() == null
        || evento.aggregateId() == null
        || !evento.aggregateId().equals(evento.payload().resultadoId()))
      throw new IllegalArgumentException("Contrato de utilización del resultado inválido");
    caso.confirmar(evento.eventId(), evento.payload().resultadoId(), evento.payload().loteId());
  }

  @KafkaListener(
      topics =
          "${avicontrol.modulo3.resultado-utilizado.topic:modulo3.resultado-sacrificio-utilizado.v1}",
      groupId = "avicontrol-modulo2-resultado-sacrificio",
      autoStartup = "${avicontrol.modulo3.resultado-utilizado.enabled:false}")
  public void procesarKafka(String payload) throws JsonProcessingException {
    procesar(json.readValue(payload, ResultadoSacrificioUtilizadoV1.class));
  }
}
