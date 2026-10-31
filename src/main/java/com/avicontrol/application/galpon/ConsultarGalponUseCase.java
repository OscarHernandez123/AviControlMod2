package com.avicontrol.application.galpon;

import com.avicontrol.application.galpon.result.GalponDetalleResult;
import com.avicontrol.application.galpon.result.IncidenciaResult;
import com.avicontrol.application.galpon.result.LoteActualResult;
import com.avicontrol.application.support.CurrentActorProvider;
import com.avicontrol.domain.exception.galpon.FechaIngresoInconsistenteException;
import com.avicontrol.domain.exception.galpon.GalponNoEncontradoException;
import com.avicontrol.domain.model.galpon.Galpon;
import com.avicontrol.domain.model.lote.Lote;
import com.avicontrol.domain.port.out.galpon.GalponQueryPort;
import com.avicontrol.domain.port.out.galpon.LoteQueryPort;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ConsultarGalponUseCase {

  private final GalponQueryPort galponQueryPort;
  private final LoteQueryPort loteQueryPort;
  private final CurrentActorProvider currentActorProvider;
  private final Clock clock;

  public ConsultarGalponUseCase(
      GalponQueryPort galponQueryPort,
      LoteQueryPort loteQueryPort,
      CurrentActorProvider currentActorProvider,
      Clock clock) {
    this.galponQueryPort = galponQueryPort;
    this.loteQueryPort = loteQueryPort;
    this.currentActorProvider = currentActorProvider;
    this.clock = clock;
  }

  public GalponDetalleResult execute(UUID galponId) {
    GalponAuthorization.requireReader(currentActorProvider.currentActor());
    Galpon galpon =
        galponQueryPort.buscarPorId(galponId).orElseThrow(GalponNoEncontradoException::new);
    Optional<Lote> lote = loteQueryPort.buscarActualPorGalponId(galponId);
    if (lote.isEmpty()) {
      return detalleSinLote(galpon);
    }
    return detalleConLote(galpon, lote.get());
  }

  private GalponDetalleResult detalleSinLote(Galpon galpon) {
    return new GalponDetalleResult(
        galpon.id(),
        galpon.nombre(),
        galpon.aforoMaximo(),
        galpon.estado(),
        null,
        List.of(
            new IncidenciaResult(
                "GALPON_SIN_LOTE_ACTUAL", "El galpón no tiene lote alojado actualmente")));
  }

  private GalponDetalleResult detalleConLote(Galpon galpon, Lote lote) {
    try {
      return new GalponDetalleResult(
          galpon.id(),
          galpon.nombre(),
          galpon.aforoMaximo(),
          galpon.estado(),
          new LoteActualResult(
              lote.id(),
              lote.poblacionActual(),
              lote.fechaIngreso(),
              lote.calcularEdad(LocalDate.now(clock))),
          List.of());
    } catch (FechaIngresoInconsistenteException exception) {
      return new GalponDetalleResult(
          galpon.id(),
          galpon.nombre(),
          galpon.aforoMaximo(),
          galpon.estado(),
          new LoteActualResult(lote.id(), lote.poblacionActual(), lote.fechaIngreso(), null),
          List.of(
              new IncidenciaResult(
                  exception.code(),
                  "No es posible calcular la edad por fecha de ingreso inconsistente")));
    }
  }
}
