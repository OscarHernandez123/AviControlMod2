package com.avicontrol.infrastructure.adapter.out.persistence.alerta;

import com.avicontrol.domain.port.out.alerta.AlertaSanitariaQueryPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AlertaSanitariaQueryAdapter implements AlertaSanitariaQueryPort {
  private final JdbcTemplate jdbc;

  public AlertaSanitariaQueryAdapter(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public long contar(String estado) {
    return jdbc.queryForObject(
        "SELECT COUNT(*) FROM alertas_sanitarias WHERE estado=?", Long.class, estado);
  }

  @Override
  public List<UUID> listarIds(String estado, int page, int size) {
    return jdbc.query(
        "SELECT id FROM alertas_sanitarias WHERE estado=? ORDER BY fecha_emision DESC,id LIMIT ? OFFSET ?",
        (rs, row) -> rs.getObject(1, UUID.class),
        estado,
        size,
        (long) page * size);
  }

  @Override
  public Optional<Datos> buscar(UUID id) {
    return jdbc
        .query(
            "SELECT a.id,a.prioridad,a.estado,a.galpon_id,a.lote_id,r.cantidad,r.porcentaje_mortalidad,r.causa_probable,r.observaciones,r.responsable FROM alertas_sanitarias a JOIN registros_mortalidad_anormal r ON r.id=a.reporte_id WHERE a.id=?",
            (rs, row) ->
                new Datos(
                    rs.getObject(1, UUID.class),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getObject(4, UUID.class),
                    rs.getObject(5, UUID.class),
                    rs.getInt(6),
                    rs.getBigDecimal(7),
                    rs.getString(8),
                    rs.getString(9),
                    rs.getString(10)),
            id)
        .stream()
        .findFirst();
  }
}
