package com.avicontrol.infrastructure.adapter.out.persistence.sacrificio.sanitario;

import com.avicontrol.domain.model.sacrificio.sanitario.EstadoOrdenSacrificioSanitario;
import com.avicontrol.domain.model.sacrificio.sanitario.OrdenSacrificioSanitario;
import com.avicontrol.domain.port.out.sacrificio.sanitario.OrdenSacrificioSanitarioRepositoryPort;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OrdenSacrificioSanitarioPersistenceAdapter
    implements OrdenSacrificioSanitarioRepositoryPort {
  private final JdbcTemplate jdbc;

  public OrdenSacrificioSanitarioPersistenceAdapter(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void crear(OrdenSacrificioSanitario o) {
    jdbc.update(
        "INSERT INTO ordenes_sacrificio_sanitario (id,diagnostico_id,galpon_id,lote_id,estado,fecha_emision,version) VALUES (?,?,?,?,?,?,?)",
        o.id(),
        o.diagnosticoId(),
        o.galponId(),
        o.loteId(),
        o.estado().name(),
        Timestamp.from(o.fechaEmision()),
        o.version());
  }

  @Override
  public Optional<OrdenSacrificioSanitario> bloquear(UUID id) {
    return jdbc
        .query("SELECT * FROM ordenes_sacrificio_sanitario WHERE id=? FOR UPDATE", this::map, id)
        .stream()
        .findFirst();
  }

  @Override
  public boolean ejecutar(OrdenSacrificioSanitario o) {
    return jdbc.update(
            "UPDATE ordenes_sacrificio_sanitario SET estado='EJECUTADA',poblacion_sacrificada=?,fecha_ejecucion=?,version=? WHERE id=? AND estado='PENDIENTE' AND version=?",
            o.poblacionSacrificada(),
            Timestamp.from(o.fechaEjecucion()),
            o.version(),
            o.id(),
            o.version() - 1)
        == 1;
  }

  private OrdenSacrificioSanitario map(ResultSet rs, int row) throws SQLException {
    var fecha = rs.getTimestamp("fecha_ejecucion");
    return new OrdenSacrificioSanitario(
        rs.getObject("id", UUID.class),
        rs.getObject("diagnostico_id", UUID.class),
        rs.getObject("galpon_id", UUID.class),
        rs.getObject("lote_id", UUID.class),
        EstadoOrdenSacrificioSanitario.valueOf(rs.getString("estado")),
        rs.getObject("poblacion_sacrificada", Integer.class),
        rs.getTimestamp("fecha_emision").toInstant(),
        fecha == null ? null : fecha.toInstant(),
        rs.getLong("version"));
  }
}
