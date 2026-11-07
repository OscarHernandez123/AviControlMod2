package com.avicontrol.infrastructure.adapter.in.event.sacrificio.productivo;

import java.time.Instant;
import java.util.UUID;

public record ResultadoSacrificioUtilizadoV1(
    UUID eventId,
    String eventType,
    int eventVersion,
    Instant occurredAt,
    Instant publishedAt,
    String producer,
    UUID correlationId,
    UUID causationId,
    String aggregateType,
    UUID aggregateId,
    Payload payload) {
  public record Payload(UUID resultadoId, UUID loteId) {}
}
