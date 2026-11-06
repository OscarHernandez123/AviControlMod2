package com.avicontrol.infrastructure.adapter.out.persistence.sanitario.enfermedad;

import com.avicontrol.domain.model.sanitario.enfermedad.Enfermedad;
import com.avicontrol.domain.model.sanitario.enfermedad.NivelRiesgo;
import com.avicontrol.domain.model.sanitario.enfermedad.TipoEnfermedad;
import com.avicontrol.domain.port.out.sanitario.enfermedad.EnfermedadRepositoryPort;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EnfermedadPersistenceAdapter implements EnfermedadRepositoryPort {
  private final JdbcTemplate jdbc;

  public EnfermedadPersistenceAdapter(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void crear(Enfermedad e) {
    jdbc.update(
        "INSERT INTO enfermedades (id, nombre, nivel_riesgo, tipo, descripcion_clinica, requiere_sacrificio_sanitario, version, fecha_creacion, fecha_actualizacion) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
        e.id(),
        e.nombre(),
        e.nivelRiesgo().name(),
        e.tipo().name(),
        e.descripcionClinica(),
        e.requiereSacrificioSanitario(),
        e.version(),
        e.fechaCreacion(),
        e.fechaActualizacion());
  }

  @Override
  public Optional<Enfermedad> buscar(UUID id) {
    return jdbc.query("SELECT * FROM enfermedades WHERE id = ?", this::mapear, id).stream()
        .findFirst();
  }

  @Override
  public boolean actualizar(Enfermedad anterior, Enfermedad e) {
    return jdbc.update(
            "UPDATE enfermedades SET nombre = ?, nivel_riesgo = ?, tipo = ?, descripcion_clinica = ?, requiere_sacrificio_sanitario = ?, version = ?, fecha_actualizacion = ? WHERE id = ? AND version = ?",
            e.nombre(),
            e.nivelRiesgo().name(),
            e.tipo().name(),
            e.descripcionClinica(),
            e.requiereSacrificioSanitario(),
            e.version(),
            e.fechaActualizacion(),
            e.id(),
            anterior.version())
        == 1;
  }

  private Enfermedad mapear(ResultSet rs, int row) throws SQLException {
    return new Enfermedad(
        rs.getObject("id", UUID.class),
        rs.getString("nombre"),
        NivelRiesgo.valueOf(rs.getString("nivel_riesgo")),
        TipoEnfermedad.valueOf(rs.getString("tipo")),
        rs.getString("descripcion_clinica"),
        rs.getBoolean("requiere_sacrificio_sanitario"),
        rs.getLong("version"),
        rs.getTimestamp("fecha_creacion").toInstant(),
        rs.getTimestamp("fecha_actualizacion").toInstant());
  }
}
