package com.avicontrol.infrastructure.adapter.out.integration.modulo1;

import com.avicontrol.domain.port.out.modulo1.GalponUpdatePort;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"local", "test"})
public class GalponUpdateAdapter implements GalponUpdatePort {
  private final JdbcTemplate jdbc;

  public GalponUpdateAdapter(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public boolean aislarSiProductiva(UUID galponId) {
    return jdbc.update(
            "UPDATE modulo1_galpon_local SET estado='AISLAMIENTO' WHERE id=? AND estado='PRODUCTIVA'",
            galponId)
        == 1;
  }

  @Override
  public boolean pasarACosechaSiProductiva(UUID galponId, UUID loteId) {
    return jdbc.update(
            "UPDATE modulo1_galpon_local SET estado='EN_COSECHA' WHERE id=? AND lote_id=? AND estado='PRODUCTIVA'",
            galponId,
            loteId)
        == 1;
  }

  @Override
  public boolean vaciarSiEnCosecha(UUID galponId, UUID loteId) {
    return jdbc.update(
            "UPDATE modulo1_galpon_local SET estado='VACIADO_SANITARIO' WHERE id=? AND lote_id=? AND estado='EN_COSECHA'",
            galponId,
            loteId)
        == 1;
  }

  @Override
  public boolean vaciarSiAislado(UUID galponId, UUID loteId) {
    return jdbc.update(
            "UPDATE modulo1_galpon_local SET estado='VACIADO_SANITARIO' WHERE id=? AND lote_id=? AND estado='AISLAMIENTO'",
            galponId,
            loteId)
        == 1;
  }
}
