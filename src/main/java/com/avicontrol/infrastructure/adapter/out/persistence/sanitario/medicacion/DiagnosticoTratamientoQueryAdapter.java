package com.avicontrol.infrastructure.adapter.out.persistence.sanitario.medicacion;

import com.avicontrol.domain.port.out.sanitario.medicacion.DiagnosticoTratamientoQueryPort;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DiagnosticoTratamientoQueryAdapter implements DiagnosticoTratamientoQueryPort {
  private final JdbcTemplate jdbc;

  public DiagnosticoTratamientoQueryAdapter(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public List<Tratamiento> consultar(UUID loteId, LocalDate fecha) {
    return jdbc.query(
        """
        SELECT d.id, d.fecha_diagnostico, d.duracion_dias, d.enfermedad_nombre,
               COALESCE(d.medicamento_nombre,
                 (SELECT r.nombre_medicamento FROM recepciones_medicamento r
                WHERE r.medicamento_id = d.medicamento_id AND r.nombre_medicamento IS NOT NULL
                ORDER BY r.fecha_ingreso DESC, r.id DESC LIMIT 1)) AS medicamento_nombre,
               d.principio_activo, d.dias_aplicacion,
               d.dosis, d.descripcion_medicacion
        FROM diagnosticos_galpon d
        WHERE d.lote_id = ? AND d.requiere_sacrificio_sanitario = FALSE
          AND d.fecha_diagnostico <= ? AND d.fecha_reintegro > ?
          AND (d.estado = 'ACTIVO' OR d.fecha_resolucion > ?)
        ORDER BY d.fecha_diagnostico, d.id
        """,
        (rs, row) ->
            new Tratamiento(
                rs.getObject("id", UUID.class),
                rs.getDate("fecha_diagnostico").toLocalDate(),
                rs.getInt("duracion_dias"),
                rs.getString("enfermedad_nombre"),
                rs.getString("medicamento_nombre"),
                rs.getString("principio_activo"),
                rs.getString("dosis"),
                rs.getString("descripcion_medicacion"),
                aplicaHoy(
                    rs.getString("dias_aplicacion"),
                    java.time.temporal.ChronoUnit.DAYS.between(
                            rs.getDate("fecha_diagnostico").toLocalDate(), fecha)
                        + 1)),
        loteId,
        fecha,
        fecha,
        fecha);
  }

  private static boolean aplicaHoy(String dias, long dia) {
    return dias == null
        || java.util.Arrays.stream(dias.split(","))
            .mapToInt(Integer::parseInt)
            .anyMatch(programado -> programado == dia);
  }
}
