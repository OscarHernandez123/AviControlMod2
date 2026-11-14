package com.avicontrol.infrastructure.adapter.out.persistence.sanitario.diagnostico;

import com.avicontrol.domain.exception.sanitario.diagnostico.DiagnosticoActivoExistenteException;
import com.avicontrol.domain.model.sanitario.diagnostico.DiagnosticoGalpon;
import com.avicontrol.domain.model.sanitario.diagnostico.EstadoDiagnostico;
import com.avicontrol.domain.port.out.sanitario.diagnostico.DiagnosticoGalponRepositoryPort;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DiagnosticoGalponPersistenceAdapter implements DiagnosticoGalponRepositoryPort {
  private final JdbcTemplate jdbc;

  public DiagnosticoGalponPersistenceAdapter(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void crear(DiagnosticoGalpon d) {
    try {
      jdbc.update(
          "INSERT INTO diagnosticos_galpon (id, galpon_id, active_galpon_id, lote_id, enfermedad_id, enfermedad_nombre, requiere_sacrificio_sanitario, medicacion_id, medicamento_id, dosis, duracion_dias, descripcion_medicacion, observaciones, fecha_diagnostico, fecha_reintegro, estado, fecha_resolucion, version, medicamento_nombre, principio_activo, dias_aplicacion) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          d.id(),
          d.galponId(),
          d.galponId(),
          d.loteId(),
          d.enfermedadId(),
          d.enfermedadNombre(),
          d.requiereSacrificioSanitario(),
          d.medicacionId(),
          d.medicamentoId(),
          d.dosis(),
          d.duracionDias(),
          d.descripcionMedicacion(),
          d.observaciones(),
          d.fechaDiagnostico(),
          d.fechaReintegro(),
          d.estado().name(),
          null,
          d.version(),
          d.medicamentoNombre(),
          d.principioActivo(),
          diasTexto(d.diasAplicacion()));
    } catch (DataIntegrityViolationException e) {
      String detail = e.getMostSpecificCause().getMessage();
      if (detail != null
          && detail.toLowerCase(Locale.ROOT).contains("uq_diagnostico_activo_galpon_enfermedad"))
        throw new DiagnosticoActivoExistenteException();
      throw e;
    }
  }

  @Override
  public Optional<DiagnosticoGalpon> buscar(UUID id) {
    return jdbc.query("SELECT * FROM diagnosticos_galpon WHERE id = ?", this::mapear, id).stream()
        .findFirst();
  }

  @Override
  public List<DiagnosticoGalpon> listar() {
    return jdbc.query(
        "SELECT * FROM diagnosticos_galpon ORDER BY fecha_diagnostico DESC, id DESC", this::mapear);
  }

  @Override
  public Optional<DiagnosticoGalpon> bloquear(UUID id) {
    return jdbc
        .query("SELECT * FROM diagnosticos_galpon WHERE id = ? FOR UPDATE", this::mapear, id)
        .stream()
        .findFirst();
  }

  @Override
  public boolean existeActivo(UUID galponId) {
    Integer count =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM diagnosticos_galpon WHERE active_galpon_id = ?",
            Integer.class,
            galponId);
    return count != null && count > 0;
  }

  @Override
  public boolean existeActivoMismaEnfermedad(UUID galponId, UUID enfermedadId) {
    Integer count =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM diagnosticos_galpon WHERE active_galpon_id = ? AND enfermedad_id = ?",
            Integer.class,
            galponId,
            enfermedadId);
    return count != null && count > 0;
  }

  @Override
  public boolean existeSanitarioActivo(UUID galponId) {
    Integer count =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM diagnosticos_galpon WHERE active_galpon_id = ? AND requiere_sacrificio_sanitario = TRUE",
            Integer.class,
            galponId);
    return count != null && count > 0;
  }

  @Override
  public boolean existeOtroActivo(UUID galponId, UUID diagnosticoId) {
    Integer count =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM diagnosticos_galpon WHERE active_galpon_id = ? AND id <> ?",
            Integer.class,
            galponId,
            diagnosticoId);
    return count != null && count > 0;
  }

  @Override
  public List<UUID> vencidos(LocalDate fecha) {
    return jdbc.query(
        "SELECT id FROM diagnosticos_galpon WHERE estado = 'ACTIVO' AND fecha_reintegro <= ? ORDER BY fecha_reintegro, id",
        (rs, row) -> rs.getObject(1, UUID.class),
        fecha);
  }

  @Override
  public boolean resolver(DiagnosticoGalpon anterior, DiagnosticoGalpon d) {
    return jdbc.update(
            "UPDATE diagnosticos_galpon SET estado = ?, active_galpon_id = NULL, fecha_resolucion = ?, version = ? WHERE id = ? AND estado = 'ACTIVO' AND version = ?",
            d.estado().name(),
            d.fechaResolucion(),
            d.version(),
            d.id(),
            anterior.version())
        == 1;
  }

  private DiagnosticoGalpon mapear(ResultSet rs, int row) throws SQLException {
    var fechaReintegro = rs.getDate("fecha_reintegro");
    var fechaResolucion = rs.getDate("fecha_resolucion");
    Integer duracion = rs.getObject("duracion_dias", Integer.class);
    return new DiagnosticoGalpon(
        rs.getObject("id", UUID.class),
        rs.getObject("galpon_id", UUID.class),
        rs.getObject("lote_id", UUID.class),
        rs.getObject("enfermedad_id", UUID.class),
        rs.getString("enfermedad_nombre"),
        rs.getBoolean("requiere_sacrificio_sanitario"),
        rs.getObject("medicacion_id", UUID.class),
        rs.getObject("medicamento_id", UUID.class),
        rs.getString("dosis"),
        duracion,
        rs.getString("descripcion_medicacion"),
        rs.getString("observaciones"),
        rs.getDate("fecha_diagnostico").toLocalDate(),
        fechaReintegro == null ? null : fechaReintegro.toLocalDate(),
        EstadoDiagnostico.valueOf(rs.getString("estado")),
        fechaResolucion == null ? null : fechaResolucion.toLocalDate(),
        rs.getLong("version"),
        rs.getString("medicamento_nombre"),
        rs.getString("principio_activo"),
        diasLista(rs.getString("dias_aplicacion")));
  }

  private static String diasTexto(List<Integer> dias) {
    return dias == null
        ? null
        : dias.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
  }

  private static List<Integer> diasLista(String texto) {
    return texto == null
        ? null
        : java.util.Arrays.stream(texto.split(",")).map(Integer::valueOf).toList();
  }
}
