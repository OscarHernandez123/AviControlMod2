package com.avicontrol.infrastructure.adapter.out.persistence.inventario;

import com.avicontrol.domain.model.inventario.ExistenciaAlimento;
import com.avicontrol.domain.model.inventario.ExistenciaMedicamento;
import com.avicontrol.domain.model.inventario.InventarioActual;
import com.avicontrol.domain.port.out.inventario.InventarioMovimientoQueryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class InventarioMovimientoQueryAdapter implements InventarioMovimientoQueryPort {
  private final JdbcTemplate jdbc;

  public InventarioMovimientoQueryAdapter(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public InventarioActual consultar(LocalDate fecha) {
    Map<String, AlimentoAcumulado> alimentos = new LinkedHashMap<>();
    jdbc.query(
        "SELECT r.id, r.alimento_id, r.nombre_alimento, r.tipo_alimento, r.fecha_vencimiento, r.estado, "
            + "COALESCE((SELECT SUM(CASE WHEN m.tipo_movimiento IN ('ENTRADA','AJUSTE_POSITIVO','AJUSTE') THEN m.cantidad ELSE -m.cantidad END) "
            + "FROM movimientos_inventario m WHERE m.recepcion_id = r.id AND m.tipo_producto = 'ALIMENTO' AND m.estado = 'CONFIRMADO'), 0) AS saldo "
            + "FROM recepciones_alimento r ORDER BY r.fecha_ingreso, r.id",
        rs -> {
          String nombre = rs.getString("nombre_alimento");
          String tipo = rs.getString("tipo_alimento");
          if (nombre == null || tipo == null) return;
          String clave =
              nombre.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT) + "|" + tipo;
          AlimentoAcumulado a = alimentos.get(clave);
          if (a == null) {
            a = new AlimentoAcumulado((UUID) rs.getObject("alimento_id"), nombre.strip(), tipo);
            alimentos.put(clave, a);
          }
          BigDecimal saldo = rs.getBigDecimal("saldo");
          LocalDate vencimiento = rs.getDate("fecha_vencimiento").toLocalDate();
          if ("CONFIRMADA".equals(rs.getString("estado"))
              && vencimiento.isAfter(fecha)
              && saldo.signum() > 0) {
            a.stock = a.stock.add(saldo);
            a.recepciones++;
            if (a.vencimiento == null || vencimiento.isBefore(a.vencimiento))
              a.vencimiento = vencimiento;
          }
        });
    List<ExistenciaAlimento> alimentosResult = new ArrayList<>();
    Map<String, BigDecimal> tarjetas = new LinkedHashMap<>();
    tarjetas.put("PRE_INICIO", BigDecimal.ZERO);
    tarjetas.put("INICIO", BigDecimal.ZERO);
    tarjetas.put("ENGORDE", BigDecimal.ZERO);
    for (AlimentoAcumulado a : alimentos.values()) {
      alimentosResult.add(
          new ExistenciaAlimento(
              a.id,
              a.nombre,
              a.tipo,
              a.recepciones,
              a.vencimiento,
              a.stock,
              BigDecimal.ZERO,
              "SIN_REQUERIMIENTO"));
      tarjetas.merge(a.tipo, a.stock, BigDecimal::add);
    }
    tarjetas.put("TOTAL", tarjetas.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));

    Map<String, MedicamentoAcumulado> medicamentos = new LinkedHashMap<>();
    jdbc.query(
        "SELECT r.medicamento_id, r.nombre_medicamento, r.presentacion, r.contenido_por_presentacion, r.unidad_contenido, r.fecha_vencimiento, r.estado, "
            + "COALESCE((SELECT SUM(CASE WHEN m.tipo_movimiento IN ('ENTRADA','AJUSTE_POSITIVO','AJUSTE') THEN m.cantidad ELSE -m.cantidad END) "
            + "FROM movimientos_inventario m WHERE m.recepcion_id = r.id AND m.tipo_producto = 'MEDICAMENTO' AND m.estado = 'CONFIRMADO'), 0) "
            + "- COALESCE((SELECT SUM(d.cantidad_unidad_base) FROM detalles_consumo_medicamento d WHERE d.recepcion_id = r.id), 0) AS saldo "
            + "FROM recepciones_medicamento r ORDER BY r.fecha_ingreso, r.id",
        rs -> {
          UUID id = (UUID) rs.getObject("medicamento_id");
          String presentacion = rs.getString("presentacion");
          BigDecimal contenido = rs.getBigDecimal("contenido_por_presentacion");
          String unidad = rs.getString("unidad_contenido");
          String clave =
              id + "|" + presentacion + "|" + contenido.stripTrailingZeros() + "|" + unidad;
          MedicamentoAcumulado m = medicamentos.get(clave);
          if (m == null) {
            m =
                new MedicamentoAcumulado(
                    id, rs.getString("nombre_medicamento"), presentacion, contenido, unidad);
            medicamentos.put(clave, m);
          }
          if ("CONFIRMADA".equals(rs.getString("estado"))
              && rs.getDate("fecha_vencimiento").toLocalDate().isAfter(fecha)) {
            BigDecimal saldo = rs.getBigDecimal("saldo");
            if (saldo.signum() > 0) m.stock = m.stock.add(saldo);
          }
        });
    List<ExistenciaMedicamento> medicamentosResult =
        medicamentos.values().stream()
            .map(
                m ->
                    new ExistenciaMedicamento(
                        m.id,
                        m.nombre,
                        m.presentacion,
                        m.stock.divide(m.contenido, 4, java.math.RoundingMode.HALF_UP),
                        m.contenido,
                        m.unidad,
                        m.stock))
            .toList();
    return new InventarioActual(alimentosResult, medicamentosResult, tarjetas);
  }

  private static final class AlimentoAcumulado {
    final UUID id;
    final String nombre;
    final String tipo;
    BigDecimal stock = BigDecimal.ZERO;
    int recepciones;
    LocalDate vencimiento;

    AlimentoAcumulado(UUID id, String nombre, String tipo) {
      this.id = id;
      this.nombre = nombre;
      this.tipo = tipo;
    }
  }

  private static final class MedicamentoAcumulado {
    final UUID id;
    final String nombre;
    final String presentacion;
    final BigDecimal contenido;
    final String unidad;
    BigDecimal stock = BigDecimal.ZERO;

    MedicamentoAcumulado(
        UUID id, String nombre, String presentacion, BigDecimal contenido, String unidad) {
      this.id = id;
      this.nombre = nombre;
      this.presentacion = presentacion;
      this.contenido = contenido;
      this.unidad = unidad;
    }
  }
}
