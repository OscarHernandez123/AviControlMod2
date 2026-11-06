package com.avicontrol.infrastructure.adapter.out.persistence.sanitario.aislamiento;

import com.avicontrol.domain.exception.sanitario.aislamiento.SolicitudAislamientoDuplicadaException;
import com.avicontrol.domain.model.sanitario.aislamiento.EstadoSolicitudAislamiento;
import com.avicontrol.domain.model.sanitario.aislamiento.SolicitudAislamiento;
import com.avicontrol.domain.port.out.sanitario.aislamiento.SolicitudAislamientoRepositoryPort;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SolicitudAislamientoPersistenceAdapter implements SolicitudAislamientoRepositoryPort {
  private final JdbcTemplate jdbc;

  public SolicitudAislamientoPersistenceAdapter(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void crear(SolicitudAislamiento s) {
    try {
      jdbc.update(
          "INSERT INTO solicitudes_aislamiento (id, galpon_id, pending_galpon_id, lote_id, motivo, signos_clinicos, poblacion_viva_reportada, edad_dias_reportada, trabajador, fecha_emision, estado, version) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          s.id(),
          s.galponId(),
          s.galponId(),
          s.loteId(),
          s.motivo(),
          s.signosClinicos(),
          s.poblacionVivaReportada(),
          s.edadDiasReportada(),
          s.trabajador(),
          s.fechaEmision(),
          s.estado().name(),
          s.version());
    } catch (DataIntegrityViolationException e) {
      String detalle = e.getMostSpecificCause().getMessage();
      if (detalle != null
          && detalle
              .toLowerCase(java.util.Locale.ROOT)
              .contains("uq_solicitud_aislamiento_pendiente"))
        throw new SolicitudAislamientoDuplicadaException();
      throw e;
    }
  }

  @Override
  public Optional<SolicitudAislamiento> buscar(UUID id) {
    return jdbc
        .query("SELECT * FROM solicitudes_aislamiento WHERE id = ?", this::mapear, id)
        .stream()
        .findFirst();
  }

  @Override
  public boolean existePendiente(UUID galponId) {
    Integer count =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM solicitudes_aislamiento WHERE galpon_id = ? AND estado = 'PENDIENTE_VALIDACION'",
            Integer.class,
            galponId);
    return count != null && count > 0;
  }

  @Override
  public List<SolicitudAislamiento> pendientes() {
    return jdbc.query(
        "SELECT * FROM solicitudes_aislamiento WHERE estado = 'PENDIENTE_VALIDACION' ORDER BY fecha_emision, id",
        this::mapear);
  }

  @Override
  public boolean resolver(SolicitudAislamiento anterior, SolicitudAislamiento s) {
    return jdbc.update(
            "UPDATE solicitudes_aislamiento SET estado = ?, pending_galpon_id = NULL, observaciones = ?, veterinario = ?, fecha_decision = ?, version = ? WHERE id = ? AND estado = 'PENDIENTE_VALIDACION' AND version = ?",
            s.estado().name(),
            s.observaciones(),
            s.veterinario(),
            s.fechaDecision(),
            s.version(),
            s.id(),
            anterior.version())
        == 1;
  }

  private SolicitudAislamiento mapear(ResultSet rs, int row) throws SQLException {
    var fechaDecision = rs.getTimestamp("fecha_decision");
    return new SolicitudAislamiento(
        rs.getObject("id", UUID.class),
        rs.getObject("galpon_id", UUID.class),
        rs.getObject("lote_id", UUID.class),
        rs.getString("motivo"),
        rs.getString("signos_clinicos"),
        rs.getInt("poblacion_viva_reportada"),
        rs.getInt("edad_dias_reportada"),
        rs.getString("trabajador"),
        rs.getTimestamp("fecha_emision").toInstant(),
        EstadoSolicitudAislamiento.valueOf(rs.getString("estado")),
        rs.getString("observaciones"),
        rs.getString("veterinario"),
        fechaDecision == null ? null : fechaDecision.toInstant(),
        rs.getLong("version"));
  }
}
