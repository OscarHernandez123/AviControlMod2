package com.avicontrol.infrastructure.adapter.out.persistence.mortalidad;

import com.avicontrol.domain.model.mortalidad.CausaMortalidad;
import com.avicontrol.domain.model.mortalidad.ClasificacionMortalidad;
import com.avicontrol.domain.model.mortalidad.RegistroMortalidad;
import com.avicontrol.domain.port.out.mortalidad.RegistroMortalidadRepositoryPort;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RegistroMortalidadPersistenceAdapter implements RegistroMortalidadRepositoryPort {
  private final JdbcTemplate jdbc;

  public RegistroMortalidadPersistenceAdapter(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void guardar(RegistroMortalidad r) {
    jdbc.update(
        "INSERT INTO registros_mortalidad (id,galpon_id,lote_id,fecha_hora_evento,fecha_hora_registro,cantidad,causa,observaciones,responsable,clasificacion,poblacion_anterior,poblacion_resultante) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
        r.id(),
        r.galponId(),
        r.loteId(),
        Timestamp.from(r.fechaHoraEvento()),
        Timestamp.from(r.fechaHoraRegistro()),
        r.cantidad(),
        r.causa().name(),
        r.observaciones(),
        r.responsable(),
        r.clasificacion().name(),
        r.poblacionAnterior(),
        r.poblacionResultante());
  }

  @Override
  public Optional<RegistroMortalidad> buscar(UUID id) {
    return jdbc
        .query(
            "SELECT * FROM registros_mortalidad WHERE id=?",
            (rs, row) ->
                new RegistroMortalidad(
                    rs.getObject("id", UUID.class),
                    rs.getObject("galpon_id", UUID.class),
                    rs.getObject("lote_id", UUID.class),
                    rs.getTimestamp("fecha_hora_evento").toInstant(),
                    rs.getTimestamp("fecha_hora_registro").toInstant(),
                    rs.getInt("cantidad"),
                    CausaMortalidad.valueOf(rs.getString("causa")),
                    rs.getString("observaciones"),
                    rs.getString("responsable"),
                    ClasificacionMortalidad.valueOf(rs.getString("clasificacion")),
                    rs.getInt("poblacion_anterior"),
                    rs.getInt("poblacion_resultante")),
            id)
        .stream()
        .findFirst();
  }

  @Override
  public int totalDia(UUID galponId, UUID loteId, java.time.Instant inicio, java.time.Instant fin) {
    Integer total =
        jdbc.queryForObject(
            "SELECT COALESCE(SUM(cantidad),0) FROM registros_mortalidad WHERE galpon_id=? AND lote_id=? AND fecha_hora_evento>=? AND fecha_hora_evento<?",
            Integer.class,
            galponId,
            loteId,
            Timestamp.from(inicio),
            Timestamp.from(fin));
    return total == null ? 0 : total;
  }
}
