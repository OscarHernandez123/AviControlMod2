package com.avicontrol.infrastructure.demo;

import com.avicontrol.domain.model.sanitario.diagnostico.DiagnosticoGalpon;
import com.avicontrol.domain.model.sanitario.diagnostico.EstadoDiagnostico;
import com.avicontrol.domain.port.out.sanitario.aislamiento.GalponLoteAislamientoQueryPort;
import com.avicontrol.domain.port.out.sanitario.diagnostico.DiagnosticoGalponRepositoryPort;
import com.avicontrol.infrastructure.adapter.out.integration.modulo1.GalponReintegroLocalAdapter;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Profile("local & !modulo1-rest")
@ConditionalOnProperty(name = "avicontrol.demo.enabled", havingValue = "true")
public class DemoReintegroService {
  private final DiagnosticoGalponRepositoryPort diagnosticos;
  private final GalponLoteAislamientoQueryPort galpones;
  private final GalponReintegroLocalAdapter reintegro;
  private final JdbcTemplate jdbc;
  private final Clock clock;

  public DemoReintegroService(
      DiagnosticoGalponRepositoryPort diagnosticos,
      GalponLoteAislamientoQueryPort galpones,
      GalponReintegroLocalAdapter reintegro,
      JdbcTemplate jdbc,
      Clock clock) {
    this.diagnosticos = diagnosticos;
    this.galpones = galpones;
    this.reintegro = reintegro;
    this.jdbc = jdbc;
    this.clock = clock;
  }

  @Transactional
  public DiagnosticoGalpon adelantar(UUID id, String motivo, String actor) {
    if (!"avicontrol_modulo2_demo"
        .equals(jdbc.queryForObject("SELECT current_database()", String.class)))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Control exclusivo de la base DEMO");
    if (motivo == null || motivo.isBlank() || motivo.length() > 300)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Motivo DEMO obligatorio (máximo 300 caracteres)");
    var d =
        diagnosticos
            .bloquear(id)
            .orElseThrow(
                () ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Diagnóstico no encontrado"));
    if (d.estado() == EstadoDiagnostico.REINTEGRADO) return d;
    if (d.estado() != EstadoDiagnostico.ACTIVO || d.requiereSacrificioSanitario())
      throw new ResponseStatusException(HttpStatus.CONFLICT, "El diagnóstico no admite reintegro");
    var contexto =
        galpones
            .consultar(d.galponId())
            .orElseThrow(
                () ->
                    new ResponseStatusException(HttpStatus.CONFLICT, "Galpón DEMO no disponible"));
    if (!d.loteId().equals(contexto.loteId())
        || !"AISLAMIENTO".equals(contexto.estado())
        || diagnosticos.existeOtroActivo(d.galponId(), d.id()))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "El galpón, lote o diagnóstico activo cambió");
    if (!reintegro.reintegrarSiAislado(d.galponId(), d.loteId()))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "El galpón cambió durante el reintegro");
    var resuelto = d.resolver(EstadoDiagnostico.REINTEGRADO, LocalDate.now(clock));
    if (!diagnosticos.resolver(d, resuelto))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "El diagnóstico cambió durante el reintegro");
    // Solo agrega la marca de simulación; no altera la prescripción ni los consumos históricos.
    String marca =
        " [DEMO: reintegro anticipado por "
            + actor
            + " a "
            + clock.instant()
            + "; "
            + motivo.strip()
            + "]";
    if (jdbc.update(
            "UPDATE diagnosticos_galpon SET observaciones = observaciones || ? WHERE id = ? AND estado = 'REINTEGRADO' AND version = ?",
            marca,
            id,
            resuelto.version())
        != 1)
      throw new ResponseStatusException(HttpStatus.CONFLICT, "No se pudo registrar la marca DEMO");
    return diagnosticos.buscar(id).orElseThrow();
  }
}
