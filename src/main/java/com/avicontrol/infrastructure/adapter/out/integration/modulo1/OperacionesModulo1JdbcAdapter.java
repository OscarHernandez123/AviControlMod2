package com.avicontrol.infrastructure.adapter.out.integration.modulo1;

import com.avicontrol.domain.port.out.modulo1.OperacionesModulo1Port;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Component
@Profile("modulo1-rest & !local & !test")
public class OperacionesModulo1JdbcAdapter implements OperacionesModulo1Port {
  private final JdbcTemplate jdbc;
  private final JsonMapper json = JsonMapper.builder().build();

  public OperacionesModulo1JdbcAdapter(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void bloquear(UUID id) {
    jdbc.queryForList("SELECT pg_advisory_xact_lock(hashtext(?))", id.toString());
  }

  public Optional<Operacion> buscar(UUID id) {
    return jdbc
        .query(
            "SELECT * FROM modulo1_operaciones WHERE id=?",
            (rs, n) ->
                new Operacion(
                    id,
                    rs.getString("tipo"),
                    rs.getObject("galpon_id", UUID.class),
                    rs.getObject("lote_id", UUID.class),
                    json.readValue(
                        rs.getString("datos"), new TypeReference<Map<String, Object>>() {})),
            id)
        .stream()
        .findFirst();
  }

  public java.util.List<Operacion> listar() {
    return jdbc
        .query(
            "SELECT id FROM modulo1_operaciones ORDER BY creado_en DESC LIMIT 50",
            (rs, n) -> rs.getObject(1, UUID.class))
        .stream()
        .map(id -> buscar(id).orElseThrow())
        .toList();
  }

  public void guardar(Operacion o) {
    jdbc.update(
        "INSERT INTO modulo1_operaciones(id,tipo,galpon_id,lote_id,datos) VALUES (?,?,?,?,?)",
        o.id(),
        o.tipo(),
        o.galponId(),
        o.loteId(),
        json.writeValueAsString(o.datos()));
  }
}
