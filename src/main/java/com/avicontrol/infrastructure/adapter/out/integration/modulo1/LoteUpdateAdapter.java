package com.avicontrol.infrastructure.adapter.out.integration.modulo1;

import com.avicontrol.domain.port.out.modulo1.LoteUpdatePort;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"local", "test"})
public class LoteUpdateAdapter implements LoteUpdatePort {
  private final JdbcTemplate jdbc;

  public LoteUpdateAdapter(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public boolean finalizarAlojamiento(UUID galponId, UUID loteId) {
    return jdbc.update(
            "UPDATE modulo1_galpon_local SET lote_id=NULL, lote_codigo=NULL, fecha_ingreso=NULL, poblacion_viva=0 WHERE id=? AND lote_id=? AND estado='VACIADO_SANITARIO'",
            galponId,
            loteId)
        == 1;
  }

  @Override
  public boolean sacrificarPoblacionSiPositiva(UUID galponId, UUID loteId) {
    return jdbc.update(
            "UPDATE modulo1_galpon_local SET poblacion_viva=0 WHERE id=? AND lote_id=? AND poblacion_viva>0 AND estado='AISLAMIENTO'",
            galponId,
            loteId)
        == 1;
  }
}
