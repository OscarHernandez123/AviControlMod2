package com.avicontrol.infrastructure.adapter.out.persistence.medicamento;

import com.avicontrol.domain.model.medicamento.ConsumoMedicamento;
import com.avicontrol.domain.model.medicamento.DetalleConsumoMedicamento;
import com.avicontrol.domain.model.medicamento.UnidadConsumoMedicamento;
import com.avicontrol.domain.port.out.medicamento.ConsumoMedicamentoRepositoryPort;
import io.micrometer.core.instrument.MeterRegistry;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ConsumoMedicamentoPersistenceAdapter implements ConsumoMedicamentoRepositoryPort {
  private final JdbcTemplate jdbc;
  private final MeterRegistry metrics;

  public ConsumoMedicamentoPersistenceAdapter(JdbcTemplate jdbc, MeterRegistry metrics) {
    this.jdbc = jdbc;
    this.metrics = metrics;
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void guardar(ConsumoMedicamento c) {
    jdbc.update(
        "INSERT INTO consumos_medicamento (id, diagnostico_id, galpon_id, lote_id, medicamento_id, cantidad_ingresada, unidad_ingresada, cantidad_unidad_base, unidad_base, fecha_aplicacion, fecha_registro, actor, idempotencia_clave) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        c.id(),
        c.diagnosticoId(),
        c.galponId(),
        c.loteId(),
        c.medicamentoId(),
        c.cantidadIngresada(),
        c.unidadIngresada().name(),
        c.cantidadUnidadBase(),
        c.unidadBase().name(),
        c.fechaAplicacion(),
        c.fechaRegistro(),
        c.actor(),
        c.idempotenciaClave());
    for (DetalleConsumoMedicamento detalle : c.detallesInventario()) {
      jdbc.update(
          "INSERT INTO detalles_consumo_medicamento (consumo_id, recepcion_id, cantidad_unidad_base, precio_neto_historico_unidad_base, moneda) VALUES (?, ?, ?, ?, ?)",
          c.id(),
          detalle.recepcionId(),
          detalle.cantidadUnidadBase(),
          detalle.precioNetoHistoricoUnidadBase(),
          detalle.moneda());
    }
  }

  @Override
  public Optional<ConsumoMedicamento> buscar(UUID id) {
    return jdbc.query("SELECT * FROM consumos_medicamento WHERE id = ?", this::mapear, id).stream()
        .findFirst();
  }

  @Override
  public Optional<ConsumoMedicamento> buscarPorIdempotencia(UUID diagnosticoId, UUID clave) {
    Optional<ConsumoMedicamento> encontrado =
        jdbc
            .query(
                "SELECT * FROM consumos_medicamento WHERE diagnostico_id = ? AND idempotencia_clave = ?",
                this::mapear,
                diagnosticoId,
                clave)
            .stream()
            .findFirst();
    if (encontrado.isPresent())
      metrics.counter("avicontrol.medicamento.consumo.reintentos").increment();
    return encontrado;
  }

  @Override
  public List<ConsumoMedicamento> listarPorLote(UUID loteId) {
    return jdbc.query(
        "SELECT * FROM consumos_medicamento WHERE lote_id = ? ORDER BY fecha_aplicacion, fecha_registro, id",
        this::mapear,
        loteId);
  }

  private ConsumoMedicamento mapear(ResultSet rs, int row) throws SQLException {
    UUID id = rs.getObject("id", UUID.class);
    List<DetalleConsumoMedicamento> detalles =
        jdbc.query(
            "SELECT recepcion_id, cantidad_unidad_base, precio_neto_historico_unidad_base, moneda FROM detalles_consumo_medicamento WHERE consumo_id = ? ORDER BY recepcion_id",
            (detail, index) ->
                new DetalleConsumoMedicamento(
                    detail.getObject("recepcion_id", UUID.class),
                    detail.getBigDecimal("cantidad_unidad_base"),
                    detail.getBigDecimal("precio_neto_historico_unidad_base"),
                    detail.getString("moneda")),
            id);
    return new ConsumoMedicamento(
        id,
        rs.getObject("diagnostico_id", UUID.class),
        rs.getObject("galpon_id", UUID.class),
        rs.getObject("lote_id", UUID.class),
        rs.getObject("medicamento_id", UUID.class),
        rs.getBigDecimal("cantidad_ingresada"),
        UnidadConsumoMedicamento.valueOf(rs.getString("unidad_ingresada")),
        rs.getBigDecimal("cantidad_unidad_base"),
        UnidadConsumoMedicamento.valueOf(rs.getString("unidad_base")),
        rs.getDate("fecha_aplicacion").toLocalDate(),
        rs.getTimestamp("fecha_registro").toInstant(),
        rs.getString("actor"),
        rs.getObject("idempotencia_clave", UUID.class),
        detalles);
  }
}
