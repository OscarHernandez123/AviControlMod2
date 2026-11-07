package com.avicontrol.infrastructure.adapter.out.persistence.alimentacion;

import com.avicontrol.application.alimentacion.ProyeccionAlimentoUseCase.HistorialAjuste;
import com.avicontrol.application.alimentacion.ProyeccionAlimentoUseCase.Proyeccion;
import com.avicontrol.application.alimentacion.port.ProyeccionAlimentoRepositoryPort;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProyeccionAlimentoPersistenceAdapter implements ProyeccionAlimentoRepositoryPort {
  private final JdbcTemplate jdbc;

  public ProyeccionAlimentoPersistenceAdapter(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void bloquearEtapa(UUID etapaId) {
    jdbc.query(
        "SELECT id FROM etapas_plan_nutricional WHERE id=? FOR UPDATE",
        rs -> {
          if (!rs.next()) throw new java.util.NoSuchElementException("Etapa no encontrada");
          return rs.getObject(1, UUID.class);
        },
        etapaId);
  }

  @Override
  public boolean existe(UUID etapaId) {
    Integer cantidad =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM proyecciones_alimento_etapa WHERE etapa_id=?",
            Integer.class,
            etapaId);
    return cantidad != null && cantidad > 0;
  }

  @Override
  public void crear(Proyeccion p) {
    jdbc.update(
        "INSERT INTO proyecciones_alimento_etapa (id,etapa_id,lote_id,galpon_id,alimento_id,etapa_crianza,tipo_alimento,estado,poblacion_inicio_etapa,cuota_kg_ave_dia,peso_nominal_bulto_kg,dias_base,dias_prorroga,proyeccion_kg,costo_unitario_kg,fuente_costo,fecha_captura_costo,advertencia_costo) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
        p.id(),
        p.etapaId(),
        p.loteId(),
        p.galponId(),
        p.alimentoId(),
        p.etapaCrianza(),
        p.tipoAlimento(),
        p.estado(),
        p.poblacionInicioEtapa(),
        p.cuotaKgAveDia(),
        p.pesoNominalPorBultoKg(),
        p.diasBase(),
        p.diasProrroga(),
        p.proyeccionKg(),
        p.costoUnitarioKg(),
        p.fuenteCosto(),
        p.fechaCapturaCosto() == null ? null : Timestamp.from(p.fechaCapturaCosto()),
        p.advertenciaCosto());
  }

  @Override
  public Proyeccion buscarPorEtapa(UUID etapaId) {
    return jdbc.query(
        "SELECT * FROM proyecciones_alimento_etapa WHERE etapa_id=?",
        rs -> rs.next() ? map(rs) : null,
        etapaId);
  }

  @Override
  public Proyeccion buscar(UUID id) {
    return jdbc.query(
        "SELECT * FROM proyecciones_alimento_etapa WHERE id=?",
        rs -> rs.next() ? map(rs) : null,
        id);
  }

  @Override
  public List<Proyeccion> listarPorLote(UUID loteId) {
    return jdbc.query(
        "SELECT * FROM proyecciones_alimento_etapa WHERE lote_id=? ORDER BY CASE etapa_crianza WHEN 'PRE_INICIO' THEN 1 WHEN 'INICIO' THEN 2 ELSE 3 END",
        (rs, row) -> map(rs),
        loteId);
  }

  @Override
  public boolean actualizarDuracion(
      UUID id, long version, int diasProrroga, BigDecimal kilogramos) {
    return jdbc.update(
            "UPDATE proyecciones_alimento_etapa SET dias_prorroga=?,proyeccion_kg=?,version=version+1 WHERE id=? AND estado='ACTIVA' AND version=?",
            diasProrroga,
            kilogramos,
            id,
            version)
        == 1;
  }

  @Override
  public void registrarAjuste(UUID proyeccionId, HistorialAjuste a) {
    jdbc.update(
        "INSERT INTO ajustes_proyeccion_alimento (id,proyeccion_id,actor,motivo,justificacion,dias_anteriores,dias_nuevos,kilogramos_anteriores,kilogramos_nuevos,fecha_registro) VALUES (?,?,?,?,?,?,?,?,?,?)",
        UUID.randomUUID(),
        proyeccionId,
        a.usuario(),
        a.motivo(),
        a.justificacion(),
        a.diasAnteriores(),
        a.diasNuevos(),
        a.kilogramosAnteriores(),
        a.kilogramosNuevos(),
        Timestamp.from(a.fechaHora()));
  }

  @Override
  public void cerrar(UUID etapaId) {
    jdbc.update(
        "UPDATE proyecciones_alimento_etapa SET estado='COMPLETADA',version=version+1 WHERE etapa_id=? AND estado='ACTIVA'",
        etapaId);
  }

  @Override
  public boolean completarCosto(UUID id, BigDecimal costo, Instant fecha) {
    return jdbc.update(
            "UPDATE proyecciones_alimento_etapa SET costo_unitario_kg=?,fuente_costo='COMPLETADO_MANUALMENTE',fecha_captura_costo=?,advertencia_costo=NULL,costo_completado_manualmente=TRUE,version=version+1 WHERE id=? AND estado='ACTIVA' AND costo_unitario_kg IS NULL",
            costo,
            Timestamp.from(fecha),
            id)
        == 1;
  }

  @Override
  public void auditarCosto(
      UUID id, BigDecimal costo, String justificacion, String actor, Instant fecha) {
    jdbc.update(
        "INSERT INTO auditoria_costo_proyeccion (id,proyeccion_id,actor,justificacion,costo_unitario_kg,fecha_registro) VALUES (?,?,?,?,?,?)",
        UUID.randomUUID(),
        id,
        actor,
        justificacion,
        costo,
        Timestamp.from(fecha));
  }

  @Override
  public List<HistorialAjuste> historial(UUID proyeccionId) {
    return jdbc.query(
        "SELECT * FROM ajustes_proyeccion_alimento WHERE proyeccion_id=? ORDER BY fecha_registro,id",
        (rs, row) ->
            new HistorialAjuste(
                rs.getTimestamp("fecha_registro").toInstant(),
                rs.getString("actor"),
                rs.getString("motivo"),
                rs.getString("justificacion"),
                rs.getInt("dias_anteriores"),
                rs.getInt("dias_nuevos"),
                rs.getBigDecimal("kilogramos_anteriores"),
                rs.getBigDecimal("kilogramos_nuevos")),
        proyeccionId);
  }

  @Override
  public String nombreAlimento(UUID alimentoId) {
    if (alimentoId == null) return null;
    return jdbc.query(
        "SELECT nombre FROM alimentos_comerciales_nutricion WHERE id=?",
        rs -> rs.next() ? rs.getString(1) : null,
        alimentoId);
  }

  @Override
  public List<CostoSaldo> saldosElegibles(UUID alimentoId, LocalDate fecha) {
    return jdbc.query(
        "SELECT r.precio_neto_kg, COALESCE((SELECT SUM(CASE WHEN m.tipo_movimiento IN ('ENTRADA','AJUSTE_POSITIVO','AJUSTE') THEN m.cantidad ELSE -m.cantidad END) FROM movimientos_inventario m WHERE m.recepcion_id=r.id AND m.estado='CONFIRMADO'),0) - COALESCE((SELECT SUM(d.cantidad_kg) FROM detalles_suministro_alimento d WHERE d.recepcion_id=r.id),0) AS saldo FROM recepciones_alimento r WHERE r.alimento_id=? AND r.estado='CONFIRMADA' AND r.fecha_vencimiento>?",
        (rs, row) -> new CostoSaldo(rs.getBigDecimal(1), rs.getBigDecimal(2)),
        alimentoId,
        fecha);
  }

  @Override
  public List<DemandaAgrupada> demandaActiva() {
    return jdbc.query(
        "SELECT alimento_id,peso_nominal_bulto_kg,SUM(proyeccion_kg) AS demanda FROM proyecciones_alimento_etapa WHERE estado='ACTIVA' AND alimento_id IS NOT NULL GROUP BY alimento_id,peso_nominal_bulto_kg",
        (rs, row) ->
            new DemandaAgrupada(
                rs.getObject("alimento_id", UUID.class),
                rs.getBigDecimal("peso_nominal_bulto_kg"),
                rs.getBigDecimal("demanda")));
  }

  @Override
  public BigDecimal stock(UUID alimentoId, BigDecimal peso, LocalDate fecha) {
    BigDecimal stock =
        jdbc.queryForObject(
            "SELECT COALESCE(SUM(GREATEST(0,COALESCE((SELECT SUM(CASE WHEN m.tipo_movimiento IN ('ENTRADA','AJUSTE_POSITIVO','AJUSTE') THEN m.cantidad ELSE -m.cantidad END) FROM movimientos_inventario m WHERE m.recepcion_id=r.id AND m.estado='CONFIRMADO'),0) - COALESCE((SELECT SUM(d.cantidad_kg) FROM detalles_suministro_alimento d WHERE d.recepcion_id=r.id),0))),0) FROM recepciones_alimento r WHERE r.alimento_id=? AND r.peso_por_bulto_kg=? AND r.estado='CONFIRMADA' AND r.fecha_vencimiento>?",
            BigDecimal.class,
            alimentoId,
            peso,
            fecha);
    return stock == null ? BigDecimal.ZERO : stock;
  }

  @Override
  public BigDecimal demandaVigente(UUID alimentoId, String nombreNormalizado, String tipoAlimento) {
    BigDecimal demanda =
        jdbc.queryForObject(
            "SELECT COALESCE(SUM(p.proyeccion_kg),0) FROM proyecciones_alimento_etapa p LEFT JOIN alimentos_comerciales_nutricion a ON a.id=p.alimento_id WHERE p.estado='ACTIVA' AND (p.alimento_id=? OR (LOWER(TRIM(a.nombre))=LOWER(TRIM(?)) AND a.tipo_alimento=?))",
            BigDecimal.class,
            alimentoId,
            nombreNormalizado,
            tipoAlimento);
    return demanda == null ? BigDecimal.ZERO : demanda;
  }

  private Proyeccion map(ResultSet rs) throws SQLException {
    return new Proyeccion(
        rs.getObject("id", UUID.class),
        rs.getObject("etapa_id", UUID.class),
        rs.getObject("lote_id", UUID.class),
        rs.getObject("galpon_id", UUID.class),
        rs.getObject("alimento_id", UUID.class),
        rs.getString("etapa_crianza"),
        rs.getString("tipo_alimento"),
        rs.getString("estado"),
        rs.getInt("poblacion_inicio_etapa"),
        rs.getBigDecimal("cuota_kg_ave_dia"),
        rs.getBigDecimal("peso_nominal_bulto_kg"),
        rs.getInt("dias_base"),
        rs.getInt("dias_prorroga"),
        rs.getBigDecimal("proyeccion_kg"),
        rs.getBigDecimal("costo_unitario_kg"),
        rs.getString("fuente_costo"),
        rs.getTimestamp("fecha_captura_costo") == null
            ? null
            : rs.getTimestamp("fecha_captura_costo").toInstant(),
        rs.getString("advertencia_costo"),
        rs.getBoolean("costo_completado_manualmente"),
        rs.getLong("version"));
  }
}
