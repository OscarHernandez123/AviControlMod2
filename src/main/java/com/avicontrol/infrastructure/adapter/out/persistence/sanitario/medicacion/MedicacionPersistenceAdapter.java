package com.avicontrol.infrastructure.adapter.out.persistence.sanitario.medicacion;

import com.avicontrol.domain.model.sanitario.medicacion.Medicacion;
import com.avicontrol.domain.port.out.sanitario.medicacion.MedicacionRepositoryPort;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MedicacionPersistenceAdapter implements MedicacionRepositoryPort {
  private final JdbcTemplate jdbc;

  public MedicacionPersistenceAdapter(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void crear(Medicacion m) {
    jdbc.update(
        "INSERT INTO medicaciones (id, enfermedad_id, medicamento_id, dosis, duracion_dias, descripcion, version, fecha_creacion, fecha_actualizacion, nombre_medicamento, principio_activo, dias_aplicacion) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        m.id(),
        m.enfermedadId(),
        m.medicamentoId(),
        m.dosis(),
        m.duracionDias(),
        m.descripcion(),
        m.version(),
        Timestamp.from(m.fechaCreacion()),
        Timestamp.from(m.fechaActualizacion()),
        m.nombreMedicamento(),
        m.principioActivo(),
        diasTexto(m.diasAplicacion()));
  }

  @Override
  public Optional<Medicacion> buscar(UUID id) {
    return jdbc.query("SELECT * FROM medicaciones WHERE id = ?", this::mapear, id).stream()
        .findFirst();
  }

  @Override
  public List<Medicacion> listar() {
    return jdbc.query("SELECT * FROM medicaciones ORDER BY fecha_creacion DESC, id", this::mapear);
  }

  @Override
  public boolean actualizar(Medicacion anterior, Medicacion m) {
    return jdbc.update(
            "UPDATE medicaciones SET enfermedad_id = ?, medicamento_id = ?, dosis = ?, duracion_dias = ?, descripcion = ?, version = ?, fecha_actualizacion = ?, nombre_medicamento = ?, principio_activo = ?, dias_aplicacion = ? WHERE id = ? AND version = ?",
            m.enfermedadId(),
            m.medicamentoId(),
            m.dosis(),
            m.duracionDias(),
            m.descripcion(),
            m.version(),
            Timestamp.from(m.fechaActualizacion()),
            m.nombreMedicamento(),
            m.principioActivo(),
            diasTexto(m.diasAplicacion()),
            m.id(),
            anterior.version())
        == 1;
  }

  private Medicacion mapear(ResultSet rs, int row) throws SQLException {
    return new Medicacion(
        rs.getObject("id", UUID.class),
        rs.getObject("enfermedad_id", UUID.class),
        rs.getObject("medicamento_id", UUID.class),
        rs.getString("dosis"),
        rs.getInt("duracion_dias"),
        rs.getString("descripcion"),
        rs.getLong("version"),
        rs.getTimestamp("fecha_creacion").toInstant(),
        rs.getTimestamp("fecha_actualizacion").toInstant(),
        rs.getString("nombre_medicamento"),
        rs.getString("principio_activo"),
        diasLista(rs.getString("dias_aplicacion")));
  }

  private static String diasTexto(java.util.List<Integer> dias) {
    return dias == null
        ? null
        : dias.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
  }

  private static java.util.List<Integer> diasLista(String texto) {
    return texto == null
        ? null
        : java.util.Arrays.stream(texto.split(",")).map(Integer::valueOf).toList();
  }
}
