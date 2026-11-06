package com.avicontrol.infrastructure.adapter.out.persistence.sanitario.medicacion;

import com.avicontrol.domain.model.sanitario.medicacion.Medicacion;
import com.avicontrol.domain.port.out.sanitario.medicacion.MedicacionRepositoryPort;
import java.sql.ResultSet;
import java.sql.SQLException;
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
        "INSERT INTO medicaciones (id, enfermedad_id, medicamento_id, dosis, duracion_dias, descripcion, version, fecha_creacion, fecha_actualizacion) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
        m.id(),
        m.enfermedadId(),
        m.medicamentoId(),
        m.dosis(),
        m.duracionDias(),
        m.descripcion(),
        m.version(),
        m.fechaCreacion(),
        m.fechaActualizacion());
  }

  @Override
  public Optional<Medicacion> buscar(UUID id) {
    return jdbc.query("SELECT * FROM medicaciones WHERE id = ?", this::mapear, id).stream()
        .findFirst();
  }

  @Override
  public boolean actualizar(Medicacion anterior, Medicacion m) {
    return jdbc.update(
            "UPDATE medicaciones SET enfermedad_id = ?, medicamento_id = ?, dosis = ?, duracion_dias = ?, descripcion = ?, version = ?, fecha_actualizacion = ? WHERE id = ? AND version = ?",
            m.enfermedadId(),
            m.medicamentoId(),
            m.dosis(),
            m.duracionDias(),
            m.descripcion(),
            m.version(),
            m.fechaActualizacion(),
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
        rs.getTimestamp("fecha_actualizacion").toInstant());
  }
}
