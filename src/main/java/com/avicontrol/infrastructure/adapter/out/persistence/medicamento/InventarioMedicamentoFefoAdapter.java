package com.avicontrol.infrastructure.adapter.out.persistence.medicamento;

import com.avicontrol.domain.exception.medicamento.ExistenciasMedicamentoInsuficientesException;
import com.avicontrol.domain.exception.medicamento.UnidadMedicamentoIncompatibleException;
import com.avicontrol.domain.model.medicamento.DetalleConsumoMedicamento;
import com.avicontrol.domain.model.medicamento.UnidadConsumoMedicamento;
import com.avicontrol.domain.port.out.medicamento.InventarioMedicamentoPort;
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
public class InventarioMedicamentoFefoAdapter implements InventarioMedicamentoPort {
  private final JdbcTemplate jdbc;
  private final MeterRegistry metrics;

  public InventarioMedicamentoFefoAdapter(JdbcTemplate jdbc, MeterRegistry metrics) {
    this.jdbc = jdbc;
    this.metrics = metrics;
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public List<DetalleConsumoMedicamento> reservarFefo(
      UUID medicamentoId,
      BigDecimal cantidadUnidadBase,
      UnidadConsumoMedicamento unidadBase,
      LocalDate fechaCorte) {
    List<RecepcionDisponible> recepciones =
        jdbc.query(
            "SELECT id, unidad_base, precio_neto_unidad_base FROM recepciones_medicamento WHERE medicamento_id = ? AND estado = 'CONFIRMADA' AND fecha_vencimiento > ? ORDER BY fecha_vencimiento, fecha_ingreso, id FOR UPDATE",
            (rs, row) ->
                new RecepcionDisponible(
                    rs.getObject("id", UUID.class),
                    rs.getString("unidad_base"),
                    rs.getBigDecimal("precio_neto_unidad_base")),
            medicamentoId,
            fechaCorte);
    BigDecimal restante = cantidadUnidadBase;
    boolean compatible = false;
    List<DetalleConsumoMedicamento> detalles = new ArrayList<>();
    for (RecepcionDisponible recepcion : recepciones) {
      if (!unidadBase.name().equals(recepcion.unidadBase())) continue;
      compatible = true;
      BigDecimal saldo =
          jdbc.queryForObject(
              "SELECT COALESCE((SELECT SUM(CASE WHEN tipo_movimiento IN ('ENTRADA','AJUSTE_POSITIVO','AJUSTE') THEN cantidad ELSE -cantidad END) FROM movimientos_inventario WHERE recepcion_id = ? AND tipo_producto = 'MEDICAMENTO' AND estado = 'CONFIRMADO'), 0) - COALESCE((SELECT SUM(cantidad_unidad_base) FROM detalles_consumo_medicamento WHERE recepcion_id = ?), 0)",
              BigDecimal.class,
              recepcion.id(),
              recepcion.id());
      if (saldo == null || saldo.signum() <= 0) continue;
      BigDecimal tomar = saldo.min(restante);
      detalles.add(
          new DetalleConsumoMedicamento(
              recepcion.id(), tomar, recepcion.precioNetoUnidadBase(), "COP"));
      restante = restante.subtract(tomar);
      if (restante.signum() == 0) break;
    }
    if (!compatible && !recepciones.isEmpty()) throw new UnidadMedicamentoIncompatibleException();
    if (restante.signum() > 0) {
      metrics
          .counter("avicontrol.medicamento.consumo.rechazos", "motivo", "existencias_insuficientes")
          .increment();
      throw new ExistenciasMedicamentoInsuficientesException();
    }
    return List.copyOf(detalles);
  }

  private record RecepcionDisponible(UUID id, String unidadBase, BigDecimal precioNetoUnidadBase) {}
}
