package com.avicontrol.application.mortalidad;

import com.avicontrol.application.mortalidad.result.RegistroMortalidadAnormalResult;
import com.avicontrol.domain.model.mortalidad.RegistroMortalidad;
import com.avicontrol.domain.model.mortalidad.RegistroMortalidadAnormal;
import com.avicontrol.domain.port.out.modulo1.LoteQueryPort;
import com.avicontrol.domain.port.out.mortalidad.GenerarAlertaSanitariaPort;
import com.avicontrol.domain.port.out.mortalidad.RegistroMortalidadAnormalRepositoryPort;
import com.avicontrol.domain.port.out.sanitario.aislamiento.GalponLoteAislamientoQueryPort;
import java.time.Clock;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class RegistrarMortalidadAnormalUseCase {
  private final GalponLoteAislamientoQueryPort galpones;
  private final LoteQueryPort lotes;
  private final RegistroMortalidadAnormalRepositoryPort reportes;
  private final GenerarAlertaSanitariaPort alertas;
  private final Clock clock;

  public RegistrarMortalidadAnormalUseCase(
      GalponLoteAislamientoQueryPort galpones,
      LoteQueryPort lotes,
      RegistroMortalidadAnormalRepositoryPort reportes,
      GenerarAlertaSanitariaPort alertas,
      Clock clock) {
    this.galpones = galpones;
    this.lotes = lotes;
    this.reportes = reportes;
    this.alertas = alertas;
    this.clock = clock;
  }

  @Transactional
  public RegistroMortalidadAnormalResult registrar(RegistrarMortalidadAnormalCommand c) {
    if (c == null
        || c.galponId() == null
        || c.fechaHoraEvento() == null
        || c.cantidad() <= 0
        || c.causaProbable() == null
        || c.causaProbable().isBlank()
        || c.responsable() == null
        || c.responsable().isBlank())
      throw new IllegalArgumentException("Reporte anormal inválido");
    if (c.idempotencyKey() != null) {
      var existente = reportes.buscar(c.idempotencyKey());
      if (existente.isPresent()) {
        var r = existente.get();
        if (!r.galponId().equals(c.galponId())
            || r.cantidad() != c.cantidad()
            || !r.fechaHoraEvento()
                .truncatedTo(java.time.temporal.ChronoUnit.MILLIS)
                .equals(c.fechaHoraEvento().truncatedTo(java.time.temporal.ChronoUnit.MILLIS))
            || !r.causaProbable().equals(c.causaProbable().strip())
            || !java.util.Objects.equals(r.observaciones(), c.observaciones())
            || !r.responsable().equals(c.responsable()))
          throw new IllegalStateException("Clave de idempotencia usada con otro reporte");
        return RegistroMortalidadAnormalResult.de(r, alertas.generar(r));
      }
    }
    var galpon =
        galpones
            .consultar(c.galponId())
            .orElseThrow(() -> new NoSuchElementException("Galpón no encontrado"));
    var lote =
        lotes
            .consultarActual(c.galponId())
            .orElseThrow(() -> new NoSuchElementException("Lote activo no encontrado"));
    if (!"PRODUCTIVA".equals(galpon.estado()) || !lote.id().equals(galpon.loteId()))
      throw new IllegalStateException("Galpón no productivo");
    Instant ahora = Instant.now(clock);
    if (lote.fechaIngreso() == null
        || c.fechaHoraEvento().isAfter(ahora)
        || c.fechaHoraEvento().atZone(clock.getZone()).toLocalDate().isBefore(lote.fechaIngreso()))
      throw new IllegalArgumentException("Fecha inválida");
    var reporte =
        RegistroMortalidadAnormal.crear(
            c.idempotencyKey() == null ? UUID.randomUUID() : c.idempotencyKey(),
            null,
            c.galponId(),
            lote.id(),
            c.fechaHoraEvento(),
            ahora,
            c.cantidad(),
            lote.poblacionActual(),
            c.causaProbable(),
            c.observaciones(),
            c.responsable());
    return guardarYAlertar(reporte);
  }

  @Transactional
  public RegistroMortalidadAnormalResult desdeMortalidad(
      RegistroMortalidad mortalidad, int poblacionBase) {
    var reporte =
        RegistroMortalidadAnormal.crear(
            UUID.randomUUID(),
            mortalidad.id(),
            mortalidad.galponId(),
            mortalidad.loteId(),
            mortalidad.fechaHoraEvento(),
            Instant.now(clock),
            mortalidad.cantidad(),
            poblacionBase,
            mortalidad.causa().name(),
            mortalidad.observaciones(),
            mortalidad.responsable());
    return guardarYAlertar(reporte);
  }

  private RegistroMortalidadAnormalResult guardarYAlertar(RegistroMortalidadAnormal r) {
    reportes.guardar(r);
    return RegistroMortalidadAnormalResult.de(r, alertas.generar(r));
  }
}
