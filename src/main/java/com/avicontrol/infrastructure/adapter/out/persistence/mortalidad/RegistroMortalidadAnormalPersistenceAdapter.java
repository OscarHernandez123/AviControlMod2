package com.avicontrol.infrastructure.adapter.out.persistence.mortalidad;

import com.avicontrol.domain.model.mortalidad.NivelSeveridadMortalidad;
import com.avicontrol.domain.model.mortalidad.RegistroMortalidadAnormal;
import com.avicontrol.domain.port.out.mortalidad.RegistroMortalidadAnormalRepositoryPort;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class RegistroMortalidadAnormalPersistenceAdapter
    implements RegistroMortalidadAnormalRepositoryPort {
  private final JdbcTemplate jdbc;

  public RegistroMortalidadAnormalPersistenceAdapter(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void bloquearGalpon(UUID galponId) {
    jdbc.queryForList(
        "SELECT id FROM modulo1_galpon_local WHERE id=? FOR UPDATE", UUID.class, galponId);
  }

  @Override
  public void guardar(RegistroMortalidadAnormal r) {
    jdbc.update(
        "INSERT INTO registros_mortalidad_anormal (id,mortalidad_id,galpon_id,lote_id,fecha_hora_evento,fecha_hora_registro,cantidad,poblacion_base,porcentaje_mortalidad,severidad,causa_probable,observaciones,responsable) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
        r.id(),
        r.mortalidadId(),
        r.galponId(),
        r.loteId(),
        Timestamp.from(r.fechaHoraEvento()),
        Timestamp.from(r.fechaHoraRegistro()),
        r.cantidad(),
        r.poblacionBase(),
        r.porcentajeMortalidad(),
        r.severidad().name(),
        r.causaProbable(),
        r.observaciones(),
        r.responsable());
  }

  @Override
  public Optional<RegistroMortalidadAnormal> buscar(UUID id) {
    return jdbc
        .query(
            "SELECT * FROM registros_mortalidad_anormal WHERE id=?",
            (rs, row) ->
                new RegistroMortalidadAnormal(
                    rs.getObject("id", UUID.class),
                    rs.getObject("mortalidad_id", UUID.class),
                    rs.getObject("galpon_id", UUID.class),
                    rs.getObject("lote_id", UUID.class),
                    rs.getTimestamp("fecha_hora_evento").toInstant(),
                    rs.getTimestamp("fecha_hora_registro").toInstant(),
                    rs.getInt("cantidad"),
                    rs.getInt("poblacion_base"),
                    rs.getBigDecimal("porcentaje_mortalidad"),
                    NivelSeveridadMortalidad.valueOf(rs.getString("severidad")),
                    rs.getString("causa_probable"),
                    rs.getString("observaciones"),
                    rs.getString("responsable")),
            id)
        .stream()
        .findFirst();
  }
}
