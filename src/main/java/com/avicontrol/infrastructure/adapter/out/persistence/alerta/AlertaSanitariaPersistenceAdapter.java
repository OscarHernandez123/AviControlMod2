package com.avicontrol.infrastructure.adapter.out.persistence.alerta;

import com.avicontrol.domain.model.alerta.AlertaSanitaria;
import com.avicontrol.domain.port.out.alerta.AlertaSanitariaRepositoryPort;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AlertaSanitariaPersistenceAdapter implements AlertaSanitariaRepositoryPort {
  private final JdbcTemplate jdbc;

  public AlertaSanitariaPersistenceAdapter(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<UUID> porReporte(UUID reporteId) {
    return jdbc
        .query(
            "SELECT id FROM alertas_sanitarias WHERE reporte_id=?",
            (rs, row) -> rs.getObject(1, UUID.class),
            reporteId)
        .stream()
        .findFirst();
  }

  @Override
  public void guardar(AlertaSanitaria a) {
    jdbc.update(
        "INSERT INTO alertas_sanitarias (id,reporte_id,galpon_id,lote_id,prioridad,estado,fecha_emision) VALUES (?,?,?,?,?,?,?)",
        a.id(),
        a.reporteId(),
        a.galponId(),
        a.loteId(),
        a.prioridad(),
        a.estado(),
        Timestamp.from(a.fechaEmision()));
  }
}
