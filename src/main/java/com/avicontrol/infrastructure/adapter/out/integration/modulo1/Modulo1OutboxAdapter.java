package com.avicontrol.infrastructure.adapter.out.integration.modulo1;

import com.avicontrol.domain.port.out.modulo1.EventosModulo1Port;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Component
@Profile("modulo1-rest & !local & !test")
public class Modulo1OutboxAdapter implements EventosModulo1Port {
  private final JdbcTemplate jdbc;
  private final JsonMapper json = JsonMapper.builder().build();

  public Modulo1OutboxAdapter(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  private String payload(Evento e) {
    return json.writeValueAsString(e.payload());
  }

  @Override
  public Optional<Entrega> repetido(Evento e) {
    return jdbc
        .query(
            "SELECT topic,payload,estado,intentos FROM modulo1_event_outbox WHERE alerta_id=?",
            (rs, n) -> {
              if (!rs.getString("topic").equals(e.topic())
                  || !rs.getString("payload").equals(payload(e)))
                throw new IllegalStateException("alertaId ya utilizado con datos distintos");
              return new Entrega(e.alertaId(), rs.getString("estado"), rs.getInt("intentos"));
            },
            e.alertaId())
        .stream()
        .findFirst();
  }

  @Override
  @Transactional
  public Entrega encolar(Evento e) {
    // Serializa solicitudes simultáneas de una misma clave, sin perder la comparación de datos.
    jdbc.queryForList("SELECT pg_advisory_xact_lock(hashtext(?))", e.alertaId().toString());
    var existente = repetido(e);
    if (existente.isPresent()) return existente.get();
    jdbc.update(
        "INSERT INTO modulo1_event_outbox(alerta_id,galpon_id,topic,event_type,occurred_at,payload) VALUES (?,?,?,?,?,?)",
        e.alertaId(),
        e.galponId(),
        e.topic(),
        e.eventType(),
        e.occurredAt(),
        payload(e));
    return new Entrega(e.alertaId(), "PENDIENTE", 0);
  }

  @Override
  public Entrega consultar(UUID id) {
    return jdbc
        .query(
            "SELECT estado,intentos FROM modulo1_event_outbox WHERE alerta_id=?",
            (rs, n) -> new Entrega(id, rs.getString("estado"), rs.getInt("intentos")),
            id)
        .stream()
        .findFirst()
        .orElseThrow(() -> new NoSuchElementException("Evento no encontrado"));
  }
}
