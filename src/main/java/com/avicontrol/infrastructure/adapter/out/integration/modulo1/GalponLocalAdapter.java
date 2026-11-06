package com.avicontrol.infrastructure.adapter.out.integration.modulo1;

import com.avicontrol.domain.model.sanitario.aislamiento.GalponLoteAislamiento;
import com.avicontrol.domain.port.out.modulo1.GalponQueryPort;
import com.avicontrol.domain.port.out.modulo1.GalponUpdatePort;
import com.avicontrol.domain.port.out.sanitario.aislamiento.GalponLoteAislamientoQueryPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"local", "test"})
public class GalponLocalAdapter
    implements GalponLoteAislamientoQueryPort, GalponQueryPort, GalponUpdatePort {
  private final JdbcTemplate jdbc;

  public GalponLocalAdapter(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<GalponLoteAislamiento> consultar(UUID id) {
    return jdbc
        .query(
            "SELECT * FROM modulo1_galpon_local WHERE id = ?",
            (rs, row) ->
                new GalponLoteAislamiento(
                    rs.getObject("id", UUID.class),
                    rs.getObject("lote_id", UUID.class),
                    rs.getString("estado"),
                    rs.getInt("poblacion_viva"),
                    rs.getDate("fecha_ingreso").toLocalDate()),
            id)
        .stream()
        .findFirst();
  }

  @Override
  public Optional<String> estadoVigente(UUID id) {
    return jdbc
        .query(
            "SELECT estado FROM modulo1_galpon_local WHERE id = ?",
            (rs, row) -> rs.getString(1),
            id)
        .stream()
        .findFirst();
  }

  @Override
  public boolean aislarSiProductiva(UUID id) {
    return jdbc.update(
            "UPDATE modulo1_galpon_local SET estado = 'AISLAMIENTO' WHERE id = ? AND estado = 'PRODUCTIVA'",
            id)
        == 1;
  }
}
