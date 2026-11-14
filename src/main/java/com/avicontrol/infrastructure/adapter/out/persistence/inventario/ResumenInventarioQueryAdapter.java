package com.avicontrol.infrastructure.adapter.out.persistence.inventario;

import com.avicontrol.domain.port.out.inventario.ResumenInventarioQueryPort;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ResumenInventarioQueryAdapter implements ResumenInventarioQueryPort {
  private final JdbcTemplate jdbc;

  public ResumenInventarioQueryAdapter(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public List<RecepcionReciente> recientes() {
    return jdbc.query(
        "SELECT codigo_lote, nombre_alimento, cantidad_bultos, precio_neto_por_bulto, peso_por_bulto_kg, fecha_ingreso "
            + "FROM recepciones_alimento WHERE estado = 'CONFIRMADA' ORDER BY fecha_ingreso DESC, id DESC LIMIT 5",
        (rs, row) ->
            new RecepcionReciente(
                rs.getString(1),
                rs.getString(2),
                rs.getInt(3),
                rs.getBigDecimal(4),
                rs.getBigDecimal(5),
                rs.getDate(6).toLocalDate()));
  }
}
