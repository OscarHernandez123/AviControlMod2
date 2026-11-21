package com.avicontrol.infrastructure.adapter.out.persistence.nutricion;

import com.avicontrol.domain.model.catalogo.EtapaCrianza;
import com.avicontrol.domain.port.out.nutricion.AlimentoCompatibleQueryPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AlimentoCompatibleQueryAdapter implements AlimentoCompatibleQueryPort {
  private final JdbcTemplate jdbc;
  private final Clock clock;

  public AlimentoCompatibleQueryAdapter(JdbcTemplate jdbc, Clock clock) {
    this.jdbc = jdbc;
    this.clock = clock;
  }

  @Override
  public List<Alimento> compatibles(EtapaCrianza tipo) {
    return jdbc.query(
        "SELECT * FROM alimentos_comerciales_nutricion WHERE tipo_alimento=? AND activo=TRUE ORDER BY nombre,peso_nominal_bulto_kg",
        (rs, row) -> map(rs),
        tipo.name());
  }

  @Override
  public Optional<Alimento> buscar(UUID id) {
    return jdbc.query(
        "SELECT * FROM alimentos_comerciales_nutricion WHERE id=? AND activo=TRUE",
        rs -> rs.next() ? Optional.of(map(rs)) : Optional.empty(),
        id);
  }

  private Alimento map(java.sql.ResultSet rs) throws java.sql.SQLException {
    UUID id = rs.getObject("id", UUID.class);
    BigDecimal peso = rs.getBigDecimal("peso_nominal_bulto_kg");
    BigDecimal stock =
        jdbc.queryForObject(
            "SELECT COALESCE(SUM(CASE WHEN r.estado='CONFIRMADA' AND r.fecha_vencimiento>? THEN GREATEST(0, "
                + "COALESCE((SELECT SUM(CASE WHEN m.tipo_movimiento IN ('ENTRADA','AJUSTE_POSITIVO','AJUSTE') THEN m.cantidad ELSE -m.cantidad END) FROM movimientos_inventario m WHERE m.recepcion_id=r.id AND m.estado='CONFIRMADO'),0)) ELSE 0 END),0) "
                + "FROM recepciones_alimento r WHERE r.alimento_id=? AND r.peso_por_bulto_kg=?",
            BigDecimal.class,
            LocalDate.now(clock),
            id,
            peso);
    if (stock == null) stock = BigDecimal.ZERO;
    return new Alimento(
        id,
        rs.getString("nombre"),
        EtapaCrianza.valueOf(rs.getString("tipo_alimento")),
        peso,
        rs.getBigDecimal("proteina_porcentaje"),
        rs.getBigDecimal("energia_kcal_kg"),
        (Integer) rs.getObject("duracion_recomendada_etapa_dias"),
        stock,
        stock.divide(peso, 4, RoundingMode.HALF_UP),
        stock.signum() > 0 ? "DISPONIBLE" : "SIN_STOCK");
  }
}
