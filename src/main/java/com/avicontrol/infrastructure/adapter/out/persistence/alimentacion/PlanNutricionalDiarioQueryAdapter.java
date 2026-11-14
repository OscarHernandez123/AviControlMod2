package com.avicontrol.infrastructure.adapter.out.persistence.alimentacion;

import com.avicontrol.domain.port.out.alimentacion.PlanNutricionalQueryPort;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PlanNutricionalDiarioQueryAdapter implements PlanNutricionalQueryPort {
  private final JdbcTemplate jdbc;
  private final Clock clock;

  public PlanNutricionalDiarioQueryAdapter(JdbcTemplate jdbc, Clock clock) {
    this.jdbc = jdbc;
    this.clock = clock;
  }

  @Override
  public Optional<EtapaDia> buscar(UUID loteId, LocalDate fecha, int edadDias) {
    return jdbc.query(
        """
        SELECT p.id AS plan_id, e.etapa_crianza, e.tipo_alimento, e.alimento_id,
               COALESCE(a.nombre,
                 (SELECT r.nombre_alimento FROM recepciones_alimento r
                  WHERE r.alimento_id = e.alimento_id AND r.nombre_alimento IS NOT NULL
                  ORDER BY r.fecha_ingreso DESC, r.id DESC LIMIT 1)) AS nombre_alimento,
               e.racion_kg_ave_dia, e.peso_nominal_bulto_kg, e.dia_inicio
        FROM planes_nutricionales_lote p
        JOIN etapas_plan_nutricional e ON e.plan_id = p.id
        LEFT JOIN alimentos_comerciales_nutricion a ON a.id = e.alimento_id
        WHERE p.lote_id = ? AND p.estado = 'ACTIVO'
          AND p.fecha_ingreso <= ? AND e.dia_inicio <= ? AND e.dia_fin >= ?
        ORDER BY e.dia_inicio DESC LIMIT 1
        """,
        rs -> {
          if (!rs.next()) return Optional.empty();
          UUID planId = rs.getObject("plan_id", UUID.class);
          Boolean auditado =
              jdbc.queryForObject(
                  "SELECT COUNT(*) > 0 FROM auditoria_planes_nutricionales WHERE plan_id = ? AND fecha_registro >= ? AND fecha_registro < ?",
                  Boolean.class,
                  planId,
                  Timestamp.from(fecha.atStartOfDay(clock.getZone()).toInstant()),
                  Timestamp.from(fecha.plusDays(1).atStartOfDay(clock.getZone()).toInstant()));
          return Optional.of(
              new EtapaDia(
                  planId,
                  rs.getString("etapa_crianza"),
                  rs.getString("tipo_alimento"),
                  rs.getObject("alimento_id", UUID.class),
                  rs.getString("nombre_alimento"),
                  rs.getBigDecimal("racion_kg_ave_dia"),
                  rs.getBigDecimal("peso_nominal_bulto_kg"),
                  edadDias == rs.getInt("dia_inicio") || Boolean.TRUE.equals(auditado)));
        },
        loteId,
        fecha,
        edadDias,
        edadDias);
  }
}
