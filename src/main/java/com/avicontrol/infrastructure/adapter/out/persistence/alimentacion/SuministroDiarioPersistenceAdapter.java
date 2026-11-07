package com.avicontrol.infrastructure.adapter.out.persistence.alimentacion;

import com.avicontrol.domain.model.alimentacion.SuministroDiarioAlimento;
import com.avicontrol.domain.model.alimentacion.SuministroDiarioAlimento.Detalle;
import com.avicontrol.domain.port.out.alimentacion.SuministroDiarioQueryPort;
import com.avicontrol.domain.port.out.alimentacion.SuministroDiarioRepositoryPort;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class SuministroDiarioPersistenceAdapter
    implements SuministroDiarioQueryPort, SuministroDiarioRepositoryPort {
  private final JdbcTemplate jdbc;
  private final MeterRegistry metrics;

  public SuministroDiarioPersistenceAdapter(JdbcTemplate jdbc, MeterRegistry metrics) {
    this.jdbc = jdbc;
    this.metrics = metrics;
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void guardar(SuministroDiarioAlimento s) {
    jdbc.update(
        "INSERT INTO suministros_diarios_alimento (id, galpon_id, lote_id, alimento_id, tipo_alimento, cantidad_kg, fecha_aplicacion, fecha_registro, trabajador, idempotencia_clave) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        s.id(),
        s.galponId(),
        s.loteId(),
        s.alimentoId(),
        s.tipoAlimento(),
        s.cantidadKg(),
        s.fechaAplicacion(),
        s.fechaRegistro(),
        s.trabajador(),
        s.idempotenciaClave());
    for (Detalle d : s.detalles()) {
      jdbc.update(
          "INSERT INTO detalles_suministro_alimento (suministro_id, recepcion_id, cantidad_kg, precio_neto_kg, moneda) VALUES (?, ?, ?, ?, 'COP')",
          s.id(),
          d.recepcionId(),
          d.cantidadKg(),
          d.precioNetoKg());
    }
  }

  @Override
  public Optional<SuministroDiarioAlimento> buscarPorClave(UUID galponId, UUID clave) {
    List<SuministroDiarioAlimento> encontrados =
        jdbc.query(
            "SELECT * FROM suministros_diarios_alimento WHERE galpon_id = ? AND idempotencia_clave = ?",
            (rs, row) -> {
              UUID id = rs.getObject("id", UUID.class);
              List<Detalle> detalles =
                  jdbc.query(
                      "SELECT recepcion_id, cantidad_kg, precio_neto_kg FROM detalles_suministro_alimento WHERE suministro_id = ? ORDER BY recepcion_id",
                      (drs, index) ->
                          new Detalle(
                              drs.getObject("recepcion_id", UUID.class),
                              drs.getBigDecimal("cantidad_kg"),
                              drs.getBigDecimal("precio_neto_kg")),
                      id);
              return new SuministroDiarioAlimento(
                  id,
                  galponId,
                  rs.getObject("lote_id", UUID.class),
                  rs.getObject("alimento_id", UUID.class),
                  rs.getString("tipo_alimento"),
                  rs.getBigDecimal("cantidad_kg"),
                  rs.getDate("fecha_aplicacion").toLocalDate(),
                  rs.getTimestamp("fecha_registro").toInstant(),
                  rs.getString("trabajador"),
                  clave,
                  detalles);
            },
            galponId,
            clave);
    if (!encontrados.isEmpty())
      metrics.counter("avicontrol.alimentacion.suministro.reintentos").increment();
    return encontrados.stream().findFirst();
  }

  @Override
  public BigDecimal acumulado(UUID loteId, UUID alimentoId, LocalDate fecha) {
    BigDecimal total =
        jdbc.queryForObject(
            "SELECT COALESCE(SUM(cantidad_kg), 0) FROM suministros_diarios_alimento WHERE lote_id = ? AND alimento_id = ? AND fecha_aplicacion = ?",
            BigDecimal.class,
            loteId,
            alimentoId,
            fecha);
    return total == null ? BigDecimal.ZERO : total;
  }
}
