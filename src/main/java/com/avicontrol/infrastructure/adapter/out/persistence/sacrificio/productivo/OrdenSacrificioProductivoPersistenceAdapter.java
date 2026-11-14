package com.avicontrol.infrastructure.adapter.out.persistence.sacrificio.productivo;

import com.avicontrol.domain.model.sacrificio.productivo.EstadoOrdenSacrificio;
import com.avicontrol.domain.model.sacrificio.productivo.OrdenSacrificioProductivo;
import com.avicontrol.domain.port.out.sacrificio.productivo.OrdenSacrificioProductivoQueryPort;
import com.avicontrol.domain.port.out.sacrificio.productivo.OrdenSacrificioProductivoRepositoryPort;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OrdenSacrificioProductivoPersistenceAdapter
    implements OrdenSacrificioProductivoRepositoryPort, OrdenSacrificioProductivoQueryPort {
  private final JdbcTemplate jdbc;

  public OrdenSacrificioProductivoPersistenceAdapter(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void crear(OrdenSacrificioProductivo orden) {
    jdbc.update(
        "INSERT INTO ordenes_sacrificio_productivo (id,galpon_id,lote_id,estado,fecha_hora_programada,fecha_creacion,version) VALUES (?,?,?,?,?,?,?)",
        orden.id(),
        orden.galponId(),
        orden.loteId(),
        orden.estado().name(),
        Timestamp.from(orden.fechaHoraProgramada()),
        Timestamp.from(orden.fechaCreacion()),
        orden.version());
  }

  @Override
  public Optional<OrdenSacrificioProductivo> buscar(UUID id) {
    return consultar(id, false);
  }

  @Override
  public Optional<OrdenSacrificioProductivo> bloquear(UUID id) {
    return consultar(id, true);
  }

  private Optional<OrdenSacrificioProductivo> consultar(UUID id, boolean bloquear) {
    return jdbc
        .query(
            "SELECT * FROM ordenes_sacrificio_productivo WHERE id=?"
                + (bloquear ? " FOR UPDATE" : ""),
            (rs, row) -> map(rs),
            id)
        .stream()
        .findFirst();
  }

  @Override
  public boolean existePorGalponLote(UUID galponId, UUID loteId) {
    Integer total =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM ordenes_sacrificio_productivo WHERE galpon_id=? AND lote_id=?",
            Integer.class,
            galponId,
            loteId);
    return total != null && total > 0;
  }

  @Override
  public boolean actualizar(OrdenSacrificioProductivo orden) {
    return jdbc.update(
            "UPDATE ordenes_sacrificio_productivo SET estado=?,fecha_hora_programada=?,fecha_reprogramacion=?,fecha_cancelacion=?,fecha_ejecucion=?,version=? WHERE id=? AND version=?",
            orden.estado().name(),
            Timestamp.from(orden.fechaHoraProgramada()),
            timestamp(orden.fechaReprogramacion()),
            timestamp(orden.fechaCancelacion()),
            timestamp(orden.fechaEjecucion()),
            orden.version(),
            orden.id(),
            orden.version() - 1)
        == 1;
  }

  @Override
  public List<UUID> vencidas(Instant ahora) {
    return jdbc.query(
        "SELECT id FROM ordenes_sacrificio_productivo WHERE estado IN ('PROGRAMADA','REPROGRAMADA') AND fecha_hora_programada<=? ORDER BY fecha_hora_programada,id LIMIT 100",
        (rs, row) -> rs.getObject(1, UUID.class),
        Timestamp.from(ahora));
  }

  @Override
  public Resumen resumen() {
    return jdbc.queryForObject(
        "SELECT COUNT(*) AS total,COALESCE(SUM(CASE WHEN estado IN ('PROGRAMADA','REPROGRAMADA') THEN 1 ELSE 0 END),0) AS pendientes FROM ordenes_sacrificio_productivo",
        (rs, row) -> new Resumen(rs.getLong("total"), rs.getLong("pendientes")));
  }

  private OrdenSacrificioProductivo map(ResultSet rs) throws SQLException {
    return new OrdenSacrificioProductivo(
        rs.getObject("id", UUID.class),
        rs.getObject("galpon_id", UUID.class),
        rs.getObject("lote_id", UUID.class),
        EstadoOrdenSacrificio.valueOf(rs.getString("estado")),
        rs.getTimestamp("fecha_hora_programada").toInstant(),
        rs.getTimestamp("fecha_creacion").toInstant(),
        instant(rs, "fecha_reprogramacion"),
        instant(rs, "fecha_cancelacion"),
        instant(rs, "fecha_ejecucion"),
        rs.getLong("version"));
  }

  private Instant instant(ResultSet rs, String column) throws SQLException {
    Timestamp timestamp = rs.getTimestamp(column);
    return timestamp == null ? null : timestamp.toInstant();
  }

  private Timestamp timestamp(Instant instant) {
    return instant == null ? null : Timestamp.from(instant);
  }
}
