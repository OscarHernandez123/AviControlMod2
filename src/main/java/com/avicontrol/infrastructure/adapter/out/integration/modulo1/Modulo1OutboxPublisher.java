package com.avicontrol.infrastructure.adapter.out.integration.modulo1;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

@Component
@Profile("modulo1-rest & !local & !test")
public class Modulo1OutboxPublisher {
  private final JdbcTemplate jdbc;
  private final KafkaTemplate<String, String> kafka;
  private final TransactionTemplate tx;
  private final Clock clock;
  private final JsonMapper json = JsonMapper.builder().build();

  public Modulo1OutboxPublisher(
      JdbcTemplate jdbc,
      @Qualifier("modulo1KafkaTemplate") KafkaTemplate<String, String> kafka,
      TransactionTemplate tx,
      Clock clock) {
    this.jdbc = jdbc;
    this.kafka = kafka;
    this.tx = tx;
    this.clock = clock;
  }

  @Scheduled(fixedDelayString = "${avicontrol.modulo1.kafka.outbox-delay-ms:1000}")
  public void publicar() {
    tx.executeWithoutResult(
        status -> {
          // Un solo publicador entre instancias. El ACK siempre precede al estado PUBLICADO.
          Boolean lock =
              jdbc.queryForObject("SELECT pg_try_advisory_xact_lock(82731022)", Boolean.class);
          if (!Boolean.TRUE.equals(lock)) return;
          var pendientes =
              jdbc.query(
                  "SELECT * FROM modulo1_event_outbox o WHERE estado='PENDIENTE' AND siguiente_intento<=CURRENT_TIMESTAMP AND NOT EXISTS (SELECT 1 FROM modulo1_event_outbox previo WHERE previo.galpon_id=o.galpon_id AND previo.estado='PENDIENTE' AND previo.secuencia<o.secuencia) ORDER BY secuencia LIMIT 20 FOR UPDATE",
                  this::map);
          for (var e : pendientes) enviar(e);
        });
  }

  private void enviar(Pendiente e) {
    var ahora = clock.instant().atZone(ZoneId.of("America/Bogota")).toOffsetDateTime();
    if (!e.fecha()
        .atZoneSameInstant(ZoneId.of("America/Bogota"))
        .toLocalDate()
        .equals(ahora.toLocalDate())) {
      jdbc.update(
          "UPDATE modulo1_event_outbox SET estado='EXPIRADO',ultimo_error='Fecha fuera del día de recepción acordado' WHERE alerta_id=?",
          e.id());
      return;
    }
    try {
      var envelope = new LinkedHashMap<String, Object>();
      envelope.put("eventId", e.id().toString());
      envelope.put("eventType", e.type());
      envelope.put("eventVersion", 1);
      envelope.put("occurredAt", e.fecha().toString());
      envelope.put("publishedAt", ahora.toString());
      envelope.put("producer", "modulo-2");
      envelope.put("aggregateType", "Galpon");
      envelope.put("aggregateId", e.galpon().toString());
      envelope.put("payload", json.readTree(e.payload()));
      kafka
          .send(e.topic(), e.galpon().toString(), json.writeValueAsString(envelope))
          .get(15, TimeUnit.SECONDS);
      jdbc.update(
          "UPDATE modulo1_event_outbox SET estado='PUBLICADO',intentos=intentos+1,publicado_en=?,ultimo_error=NULL WHERE alerta_id=?",
          ahora,
          e.id());
    } catch (Exception ex) {
      if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
      // No se alteran alertaId ni la fecha original. Si falla el commit, la siguiente entrega puede
      // duplicarse.
      jdbc.update(
          "UPDATE modulo1_event_outbox SET intentos=intentos+1,siguiente_intento=CURRENT_TIMESTAMP+INTERVAL '5 seconds',ultimo_error=? WHERE alerta_id=?",
          ex.getClass().getSimpleName(),
          e.id());
    }
  }

  private Pendiente map(ResultSet rs, int row) throws SQLException {
    return new Pendiente(
        rs.getObject("alerta_id", UUID.class),
        rs.getObject("galpon_id", UUID.class),
        rs.getString("topic"),
        rs.getString("event_type"),
        rs.getObject("occurred_at", OffsetDateTime.class),
        rs.getString("payload"));
  }

  private record Pendiente(
      UUID id, UUID galpon, String topic, String type, OffsetDateTime fecha, String payload) {}
}
