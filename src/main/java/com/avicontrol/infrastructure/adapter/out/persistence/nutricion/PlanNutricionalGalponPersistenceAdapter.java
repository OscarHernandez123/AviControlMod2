package com.avicontrol.infrastructure.adapter.out.persistence.nutricion;

import com.avicontrol.domain.model.catalogo.EtapaCrianza;
import com.avicontrol.domain.model.nutricion.AuditoriaPlanNutricional;
import com.avicontrol.domain.model.nutricion.PlanNutricionalGalpon;
import com.avicontrol.domain.port.out.nutricion.PlanNutricionalGalponRepositoryPort;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PlanNutricionalGalponPersistenceAdapter
    implements PlanNutricionalGalponRepositoryPort {
  private final JdbcTemplate jdbc;

  public PlanNutricionalGalponPersistenceAdapter(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void crear(PlanNutricionalGalpon p) {
    jdbc.update(
        "INSERT INTO planes_nutricionales_lote (id,galpon_id,lote_id,codigo_lote,plantilla_id,tipo_ave,fecha_ingreso,edad_ingreso_dias,poblacion_viva,fecha_salida_proyectada,estado) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
        p.id(),
        p.galponId(),
        p.loteId(),
        p.codigoLote(),
        p.plantillaId(),
        p.tipoAve(),
        Date.valueOf(p.fechaIngreso()),
        p.edadIngresoDias(),
        p.poblacionViva(),
        Date.valueOf(p.fechaSalidaProyectada()),
        p.estado());
    for (PlanNutricionalGalpon.Etapa e : p.etapas()) insertarEtapa(p.id(), e);
  }

  @Override
  public void actualizar(PlanNutricionalGalpon p) {
    jdbc.update(
        "UPDATE planes_nutricionales_lote SET plantilla_id=?, codigo_lote=?, poblacion_viva=?, fecha_salida_proyectada=?, estado=?, version=version+1 WHERE id=?",
        p.plantillaId(),
        p.codigoLote(),
        p.poblacionViva(),
        Date.valueOf(p.fechaSalidaProyectada()),
        p.estado(),
        p.id());
    for (PlanNutricionalGalpon.Etapa e : p.etapas()) {
      int count =
          jdbc.update(
              "UPDATE etapas_plan_nutricional SET estado=?,dia_inicio=?,dia_fin=?,racion_kg_ave_dia=?,tipo_alimento=?,alimento_id=?,peso_nominal_bulto_kg=? WHERE id=?",
              e.estado(),
              e.diaInicio(),
              e.diaFin(),
              e.racionKgAveDia(),
              e.tipoAlimento().name(),
              e.alimentoId(),
              e.pesoNominalPorBultoKg(),
              e.id());
      if (count == 0) insertarEtapa(p.id(), e);
    }
  }

  private void insertarEtapa(UUID planId, PlanNutricionalGalpon.Etapa e) {
    jdbc.update(
        "INSERT INTO etapas_plan_nutricional (id,plan_id,etapa_crianza,estado,dia_inicio,dia_fin,racion_kg_ave_dia,tipo_alimento,alimento_id,peso_nominal_bulto_kg) VALUES (?,?,?,?,?,?,?,?,?,?)",
        e.id(),
        planId,
        e.etapaCrianza().name(),
        e.estado(),
        e.diaInicio(),
        e.diaFin(),
        e.racionKgAveDia(),
        e.tipoAlimento().name(),
        e.alimentoId(),
        e.pesoNominalPorBultoKg());
  }

  @Override
  public Optional<PlanNutricionalGalpon> buscarPorGalpon(UUID galponId) {
    return jdbc.query(
        "SELECT id FROM planes_nutricionales_lote WHERE galpon_id=? ORDER BY fecha_ingreso DESC LIMIT 1",
        rs -> rs.next() ? Optional.of(map(rs.getObject(1, UUID.class))) : Optional.empty(),
        galponId);
  }

  @Override
  public Optional<PlanNutricionalGalpon> buscarPorLote(UUID loteId) {
    return jdbc.query(
        "SELECT id FROM planes_nutricionales_lote WHERE lote_id=?",
        rs -> rs.next() ? Optional.of(map(rs.getObject(1, UUID.class))) : Optional.empty(),
        loteId);
  }

  @Override
  public Optional<PlanNutricionalGalpon.Etapa> buscarEtapa(UUID etapaId) {
    return jdbc.query(
        "SELECT * FROM etapas_plan_nutricional WHERE id=?",
        rs -> rs.next() ? Optional.of(mapEtapa(rs)) : Optional.empty(),
        etapaId);
  }

  private PlanNutricionalGalpon map(UUID id) {
    List<PlanNutricionalGalpon.Etapa> etapas =
        jdbc.query(
            "SELECT * FROM etapas_plan_nutricional WHERE plan_id=? ORDER BY dia_inicio",
            (rs, row) -> mapEtapa(rs),
            id);
    return jdbc.queryForObject(
        "SELECT * FROM planes_nutricionales_lote WHERE id=?",
        (rs, row) ->
            new PlanNutricionalGalpon(
                id,
                rs.getObject("galpon_id", UUID.class),
                rs.getObject("lote_id", UUID.class),
                rs.getString("codigo_lote"),
                rs.getObject("plantilla_id", UUID.class),
                rs.getString("tipo_ave"),
                rs.getDate("fecha_ingreso").toLocalDate(),
                rs.getInt("edad_ingreso_dias"),
                rs.getInt("poblacion_viva"),
                rs.getDate("fecha_salida_proyectada").toLocalDate(),
                rs.getString("estado"),
                etapas),
        id);
  }

  private PlanNutricionalGalpon.Etapa mapEtapa(java.sql.ResultSet rs) throws java.sql.SQLException {
    return new PlanNutricionalGalpon.Etapa(
        rs.getObject("id", UUID.class),
        EtapaCrianza.valueOf(rs.getString("etapa_crianza")),
        rs.getString("estado"),
        rs.getInt("dia_inicio"),
        rs.getInt("dia_fin"),
        rs.getBigDecimal("racion_kg_ave_dia"),
        EtapaCrianza.valueOf(rs.getString("tipo_alimento")),
        rs.getObject("alimento_id", UUID.class),
        rs.getBigDecimal("peso_nominal_bulto_kg"));
  }

  @Override
  public void registrarAuditoria(UUID planId, AuditoriaPlanNutricional a) {
    jdbc.update(
        "INSERT INTO auditoria_planes_nutricionales (id,plan_id,etapa_id,accion,actor,justificacion,valor_anterior,valor_nuevo,fecha_registro) VALUES (?,?,?,?,?,?,?,?,?)",
        UUID.randomUUID(),
        planId,
        a.etapaId(),
        a.accion(),
        a.actor(),
        a.justificacion(),
        a.valorAnterior(),
        a.valorNuevo(),
        Timestamp.from(a.fechaRegistro()));
  }

  @Override
  public void registrarAjuste(
      UUID etapaId,
      String actor,
      String motivo,
      String justificacion,
      int diaFinAnterior,
      int diaFinNuevo,
      java.time.Instant fecha) {
    jdbc.update(
        "INSERT INTO ajustes_etapa_nutricional (id,etapa_id,actor,motivo,justificacion,dia_fin_anterior,dia_fin_nuevo,fecha_registro) VALUES (?,?,?,?,?,?,?,?)",
        UUID.randomUUID(),
        etapaId,
        actor,
        motivo,
        justificacion,
        diaFinAnterior,
        diaFinNuevo,
        Timestamp.from(fecha));
  }

  @Override
  public List<AuditoriaPlanNutricional> historial(UUID planId) {
    return jdbc.query(
        "SELECT * FROM auditoria_planes_nutricionales WHERE plan_id=? ORDER BY fecha_registro,id",
        (rs, row) ->
            new AuditoriaPlanNutricional(
                rs.getObject("etapa_id", UUID.class),
                rs.getString("accion"),
                rs.getString("actor"),
                rs.getString("justificacion"),
                rs.getString("valor_anterior"),
                rs.getString("valor_nuevo"),
                rs.getTimestamp("fecha_registro").toInstant()),
        planId);
  }

  @Override
  public List<UUID> lotesActivos() {
    return jdbc.query(
        "SELECT lote_id FROM planes_nutricionales_lote WHERE estado='ACTIVO'",
        (rs, row) -> rs.getObject(1, UUID.class));
  }
}
