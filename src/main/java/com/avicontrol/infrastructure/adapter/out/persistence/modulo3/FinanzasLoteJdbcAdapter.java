package com.avicontrol.infrastructure.adapter.out.persistence.modulo3;

import com.avicontrol.domain.model.recepcion.RecepcionCalculos;
import com.avicontrol.domain.port.out.modulo3.FinanzasLoteQueryPort;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class FinanzasLoteJdbcAdapter implements FinanzasLoteQueryPort {
  private final JdbcTemplate jdbc;

  public FinanzasLoteJdbcAdapter(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public List<PartidaAlimento> alimentos(UUID loteId) {
    return jdbc.query(
        """
        SELECT s.id suministro_id,d.recepcion_id,s.galpon_id,s.alimento_id,
          r.nombre_alimento,s.tipo_alimento,s.fecha_aplicacion,d.cantidad_kg,
          d.precio_neto_kg,r.impuesto_porcentaje,r.valor_impuesto impuesto_recepcion,d.moneda
        FROM suministros_diarios_alimento s
        JOIN detalles_suministro_alimento d ON d.suministro_id=s.id
        JOIN recepciones_alimento r ON r.id=d.recepcion_id
        WHERE s.lote_id=?
        ORDER BY s.fecha_aplicacion,s.fecha_registro,s.id,d.recepcion_id
        """,
        (rs, row) ->
            new PartidaAlimento(
                rs.getObject("suministro_id", UUID.class),
                rs.getObject("recepcion_id", UUID.class),
                rs.getObject("galpon_id", UUID.class),
                rs.getObject("alimento_id", UUID.class),
                rs.getString("nombre_alimento"),
                rs.getString("tipo_alimento"),
                rs.getDate("fecha_aplicacion").toLocalDate(),
                rs.getBigDecimal("cantidad_kg"),
                rs.getBigDecimal("precio_neto_kg"),
                rs.getBigDecimal("impuesto_porcentaje"),
                impuestoPartida(
                    rs.getBigDecimal("cantidad_kg"),
                    rs.getBigDecimal("precio_neto_kg"),
                    rs.getBigDecimal("impuesto_porcentaje")),
                rs.getBigDecimal("impuesto_recepcion"),
                rs.getString("moneda"),
                nombreDisponible(rs.getString("nombre_alimento"))),
        loteId);
  }

  @Override
  public List<PartidaMedicamento> medicamentos(UUID loteId) {
    return jdbc.query(
        """
        SELECT c.id consumo_id,d.recepcion_id,c.galpon_id,c.medicamento_id,
          r.nombre_medicamento,c.fecha_aplicacion,d.cantidad_unidad_base,c.unidad_base,
          d.precio_neto_historico_unidad_base,r.impuesto_porcentaje,r.valor_impuesto impuesto_recepcion,d.moneda
        FROM consumos_medicamento c
        JOIN detalles_consumo_medicamento d ON d.consumo_id=c.id
        JOIN recepciones_medicamento r ON r.id=d.recepcion_id
        WHERE c.lote_id=?
        ORDER BY c.fecha_aplicacion,c.fecha_registro,c.id,d.recepcion_id
        """,
        (rs, row) ->
            new PartidaMedicamento(
                rs.getObject("consumo_id", UUID.class),
                rs.getObject("recepcion_id", UUID.class),
                rs.getObject("galpon_id", UUID.class),
                rs.getObject("medicamento_id", UUID.class),
                rs.getString("nombre_medicamento"),
                rs.getDate("fecha_aplicacion").toLocalDate(),
                rs.getBigDecimal("cantidad_unidad_base"),
                rs.getString("unidad_base"),
                rs.getBigDecimal("precio_neto_historico_unidad_base"),
                rs.getBigDecimal("impuesto_porcentaje"),
                impuestoPartida(
                    rs.getBigDecimal("cantidad_unidad_base"),
                    rs.getBigDecimal("precio_neto_historico_unidad_base"),
                    rs.getBigDecimal("impuesto_porcentaje")),
                rs.getBigDecimal("impuesto_recepcion"),
                rs.getString("moneda"),
                nombreDisponible(rs.getString("nombre_medicamento"))),
        loteId);
  }

  private static BigDecimal impuestoPartida(
      BigDecimal cantidad, BigDecimal precio, BigDecimal porcentaje) {
    return RecepcionCalculos.moneda(
        cantidad.multiply(precio).multiply(porcentaje).movePointLeft(2));
  }

  private static boolean nombreDisponible(String nombre) {
    return nombre != null && !nombre.isBlank();
  }
}
