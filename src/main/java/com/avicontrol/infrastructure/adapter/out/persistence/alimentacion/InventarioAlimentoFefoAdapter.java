package com.avicontrol.infrastructure.adapter.out.persistence.alimentacion;

import com.avicontrol.domain.exception.alimentacion.ExistenciasAlimentoInsuficientesException;
import com.avicontrol.domain.model.alimentacion.SuministroDiarioAlimento.Detalle;
import com.avicontrol.domain.port.out.alimentacion.InventarioAlimentoPort;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class InventarioAlimentoFefoAdapter implements InventarioAlimentoPort {
  private final JdbcTemplate jdbc;
  private final MeterRegistry metrics;

  public InventarioAlimentoFefoAdapter(JdbcTemplate jdbc, MeterRegistry metrics) {
    this.jdbc = jdbc;
    this.metrics = metrics;
  }

  @Override
  @Transactional(readOnly = true)
  public BigDecimal disponible(UUID alimentoId, String tipoAlimento, LocalDate fecha) {
    BigDecimal total =
        jdbc.queryForObject(
            """
        SELECT COALESCE(SUM(
          COALESCE((SELECT SUM(CASE WHEN m.tipo_movimiento IN ('ENTRADA','AJUSTE_POSITIVO','AJUSTE')
              THEN m.cantidad ELSE -m.cantidad END)
            FROM movimientos_inventario m WHERE m.recepcion_id = r.id
              AND m.tipo_producto = 'ALIMENTO' AND m.estado = 'CONFIRMADO'), 0)
          - COALESCE((SELECT SUM(d.cantidad_kg) FROM detalles_suministro_alimento d
            WHERE d.recepcion_id = r.id), 0)), 0)
        FROM recepciones_alimento r
        WHERE r.alimento_id = ? AND r.tipo_alimento = ?
          AND r.estado = 'CONFIRMADA' AND r.fecha_vencimiento > ?
        """,
            BigDecimal.class,
            alimentoId,
            tipoAlimento,
            fecha);
    return total == null ? BigDecimal.ZERO : total.max(BigDecimal.ZERO);
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public List<Detalle> reservarFefo(
      UUID alimentoId, String tipoAlimento, BigDecimal cantidadKg, LocalDate fecha) {
    List<Recepcion> recepciones =
        jdbc.query(
            """
        SELECT id, precio_neto_kg FROM recepciones_alimento
        WHERE alimento_id = ? AND tipo_alimento = ? AND estado = 'CONFIRMADA'
          AND fecha_vencimiento > ?
        ORDER BY fecha_vencimiento, fecha_ingreso, id FOR UPDATE
        """,
            (rs, row) ->
                new Recepcion(rs.getObject("id", UUID.class), rs.getBigDecimal("precio_neto_kg")),
            alimentoId,
            tipoAlimento,
            fecha);
    BigDecimal restante = cantidadKg;
    List<Detalle> detalles = new ArrayList<>();
    for (Recepcion recepcion : recepciones) {
      BigDecimal saldo =
          jdbc.queryForObject(
              """
          SELECT COALESCE((SELECT SUM(CASE WHEN tipo_movimiento IN ('ENTRADA','AJUSTE_POSITIVO','AJUSTE')
              THEN cantidad ELSE -cantidad END) FROM movimientos_inventario
              WHERE recepcion_id = ? AND tipo_producto = 'ALIMENTO' AND estado = 'CONFIRMADO'), 0)
            - COALESCE((SELECT SUM(cantidad_kg) FROM detalles_suministro_alimento
              WHERE recepcion_id = ?), 0)
          """,
              BigDecimal.class,
              recepcion.id(),
              recepcion.id());
      if (saldo == null || saldo.signum() <= 0) continue;
      BigDecimal tomar = saldo.min(restante);
      detalles.add(new Detalle(recepcion.id(), tomar, recepcion.precioNetoKg()));
      restante = restante.subtract(tomar);
      if (restante.signum() == 0) break;
    }
    if (restante.signum() > 0) {
      metrics
          .counter(
              "avicontrol.alimentacion.suministro.rechazos", "motivo", "existencias_insuficientes")
          .increment();
      throw new ExistenciasAlimentoInsuficientesException();
    }
    return List.copyOf(detalles);
  }

  private record Recepcion(UUID id, BigDecimal precioNetoKg) {}
}
