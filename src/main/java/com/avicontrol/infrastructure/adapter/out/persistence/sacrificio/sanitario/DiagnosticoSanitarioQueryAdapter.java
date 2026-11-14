package com.avicontrol.infrastructure.adapter.out.persistence.sacrificio.sanitario;

import com.avicontrol.domain.port.out.sacrificio.sanitario.DiagnosticoSanitarioQueryPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DiagnosticoSanitarioQueryAdapter implements DiagnosticoSanitarioQueryPort {
  private final JdbcTemplate jdbc;

  public DiagnosticoSanitarioQueryAdapter(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<DiagnosticoDatos> activoPorGalpon(UUID galponId) {
    return jdbc
        .query(
            "SELECT d.id,d.galpon_id,d.lote_id,e.requiere_sacrificio_sanitario FROM diagnosticos_galpon d JOIN enfermedades e ON e.id=d.enfermedad_id WHERE d.active_galpon_id=? AND d.estado='ACTIVO' ORDER BY d.fecha_diagnostico DESC,d.id DESC",
            (rs, row) ->
                new DiagnosticoDatos(
                    rs.getObject(1, UUID.class),
                    rs.getObject(2, UUID.class),
                    rs.getObject(3, UUID.class),
                    rs.getBoolean(4)),
            galponId)
        .stream()
        .findFirst();
  }
}
