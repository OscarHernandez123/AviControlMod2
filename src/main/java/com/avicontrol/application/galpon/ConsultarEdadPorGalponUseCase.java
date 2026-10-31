package com.avicontrol.application.galpon;

import com.avicontrol.application.galpon.result.EdadLoteResult;
import com.avicontrol.application.support.CurrentActorProvider;
import com.avicontrol.domain.exception.galpon.GalponNoEncontradoException;
import com.avicontrol.domain.model.lote.Lote;
import com.avicontrol.domain.port.out.galpon.GalponQueryPort;
import com.avicontrol.domain.port.out.galpon.LoteQueryPort;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public final class ConsultarEdadPorGalponUseCase {

  private static final String GALPON_SIN_LOTE = "El galpón no tiene lote alojado actualmente";

  private final GalponQueryPort galponQueryPort;
  private final LoteQueryPort loteQueryPort;
  private final CurrentActorProvider currentActorProvider;
  private final Clock clock;

  public ConsultarEdadPorGalponUseCase(
      GalponQueryPort galponQueryPort,
      LoteQueryPort loteQueryPort,
      CurrentActorProvider currentActorProvider,
      Clock clock) {
    this.galponQueryPort = galponQueryPort;
    this.loteQueryPort = loteQueryPort;
    this.currentActorProvider = currentActorProvider;
    this.clock = clock;
  }

  public EdadLoteResult execute(UUID galponId) {
    GalponAuthorization.requireAdministrator(currentActorProvider.currentActor());
    galponQueryPort.buscarPorId(galponId).orElseThrow(GalponNoEncontradoException::new);
    Optional<Lote> lote = loteQueryPort.buscarActualPorGalponId(galponId);
    if (lote.isEmpty()) {
      return new EdadLoteResult(galponId, null, null, GALPON_SIN_LOTE);
    }
    return new EdadLoteResult(
        galponId, lote.get().id(), lote.get().calcularEdad(LocalDate.now(clock)), null);
  }
}
