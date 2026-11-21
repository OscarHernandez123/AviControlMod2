package com.avicontrol.domain.port.out.modulo1;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface EventosModulo1Port {
  record Evento(
      UUID alertaId,
      UUID galponId,
      String topic,
      String eventType,
      OffsetDateTime occurredAt,
      Map<String, Object> payload) {}

  record Entrega(UUID alertaId, String estado, int intentos) {}

  Optional<Entrega> repetido(Evento evento);

  Entrega encolar(Evento evento);

  Entrega consultar(UUID alertaId);
}
