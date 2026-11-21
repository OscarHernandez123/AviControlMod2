package com.avicontrol.infrastructure.adapter.out.integration.modulo1;

import com.avicontrol.domain.port.out.sanitario.diagnostico.GalponReintegroPort;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"local", "test"})
public class GalponReintegroLocalAdapter implements GalponReintegroPort {
  private final JdbcTemplate jdbc;

  public GalponReintegroLocalAdapter(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public boolean reintegrarSiAislado(UUID galponId, UUID loteId) {
    return jdbc.update(
            "UPDATE modulo1_galpon_local SET estado = 'PRODUCTIVA' WHERE id = ? AND lote_id = ? AND estado = 'AISLAMIENTO'",
            galponId,
            loteId)
        == 1;
  }
}
