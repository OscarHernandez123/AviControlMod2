package com.avicontrol.infrastructure.adapter.out.integration.modulo1;

import com.avicontrol.domain.model.lote.Lote;
import com.avicontrol.domain.port.out.modulo1.LoteQueryPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"local", "test"})
public class LoteQueryAdapter implements LoteQueryPort {
  private final JdbcTemplate jdbc;

  public LoteQueryAdapter(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<Lote> consultarActual(UUID galponId) {
    return jdbc
        .query(
            "SELECT lote_id,id,poblacion_viva,fecha_ingreso FROM modulo1_galpon_local WHERE id = ? AND lote_id IS NOT NULL",
            (rs, row) ->
                new Lote(
                    rs.getObject("lote_id", UUID.class),
                    rs.getObject("id", UUID.class),
                    rs.getInt("poblacion_viva"),
                    rs.getDate("fecha_ingreso") == null
                        ? null
                        : rs.getDate("fecha_ingreso").toLocalDate()),
            galponId)
        .stream()
        .findFirst();
  }
}
