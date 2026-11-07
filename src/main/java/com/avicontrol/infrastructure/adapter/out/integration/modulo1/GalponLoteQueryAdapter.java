package com.avicontrol.infrastructure.adapter.out.integration.modulo1;

import com.avicontrol.domain.port.out.sanitario.medicacion.GalponLoteQueryPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"local", "test"})
public class GalponLoteQueryAdapter implements GalponLoteQueryPort {
  private final JdbcTemplate jdbc;

  public GalponLoteQueryAdapter(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<GalponLote> consultar(UUID galponId) {
    return jdbc
        .query(
            "SELECT * FROM modulo1_galpon_local WHERE id = ?",
            (rs, row) ->
                new GalponLote(
                    rs.getObject("id", UUID.class),
                    rs.getString("nombre"),
                    rs.getInt("aforo_maximo"),
                    rs.getString("estado"),
                    rs.getObject("lote_id", UUID.class),
                    rs.getString("lote_codigo"),
                    rs.getInt("poblacion_viva"),
                    rs.getDate("fecha_ingreso") == null
                        ? null
                        : rs.getDate("fecha_ingreso").toLocalDate()),
            galponId)
        .stream()
        .findFirst();
  }
}
