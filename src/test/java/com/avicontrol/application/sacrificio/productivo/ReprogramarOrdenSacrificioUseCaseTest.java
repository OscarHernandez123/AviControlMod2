package com.avicontrol.application.sacrificio.productivo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.avicontrol.domain.exception.sacrificio.productivo.PrecondicionSacrificioNoCumplidaException;
import com.avicontrol.domain.model.lote.Lote;
import com.avicontrol.domain.model.sacrificio.productivo.OrdenSacrificioProductivo;
import com.avicontrol.domain.port.out.modulo1.GalponQueryPort;
import com.avicontrol.domain.port.out.modulo1.LoteQueryPort;
import com.avicontrol.domain.port.out.sacrificio.productivo.OrdenSacrificioProductivoRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReprogramarOrdenSacrificioUseCaseTest {
  @Mock OrdenSacrificioProductivoRepositoryPort ordenes;
  @Mock GalponQueryPort galpones;
  @Mock LoteQueryPort lotes;

  private final Clock clock =
      Clock.fixed(Instant.parse("2026-11-20T15:00:00Z"), ZoneId.of("America/Bogota"));

  @Test
  void reprogramaOrdenCuandoGalponSigueEnCosechaYActualizaOptimista() {
    ReprogramarOrdenSacrificioUseCase useCase =
        new ReprogramarOrdenSacrificioUseCase(ordenes, galpones, lotes, clock);
    UUID galponId = UUID.randomUUID();
    UUID loteId = UUID.randomUUID();
    Instant ahora = Instant.now(clock);
    OrdenSacrificioProductivo orden =
        OrdenSacrificioProductivo.programar(galponId, loteId, ahora.plusSeconds(3600), ahora);
    Instant nuevaFecha = ahora.plusSeconds(10800);

    when(ordenes.bloquear(orden.id())).thenReturn(Optional.of(orden));
    when(galpones.consultar(galponId))
        .thenReturn(
            Optional.of(new GalponQueryPort.GalponDatos(galponId, "G1", 1000, "EN_COSECHA")));
    when(lotes.consultarActual(galponId))
        .thenReturn(Optional.of(new Lote(loteId, galponId, 950, LocalDate.of(2026, 10, 10))));
    when(ordenes.actualizar(any())).thenReturn(true);

    var result = useCase.reprogramar(orden.id(), nuevaFecha);

    assertThat(result.estado()).isEqualTo("REPROGRAMADA");
    assertThat(result.fechaHoraProgramada()).isEqualTo(nuevaFecha);
  }

  @Test
  void rechazaSiOrdenNoExisteOConcurrenciaOptimistaFalla() {
    ReprogramarOrdenSacrificioUseCase useCase =
        new ReprogramarOrdenSacrificioUseCase(ordenes, galpones, lotes, clock);
    UUID id = UUID.randomUUID();
    Instant ahora = Instant.now(clock);

    when(ordenes.bloquear(id)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> useCase.reprogramar(id, ahora.plusSeconds(3600)))
        .isInstanceOf(NoSuchElementException.class);

    UUID galponId = UUID.randomUUID();
    UUID loteId = UUID.randomUUID();
    OrdenSacrificioProductivo orden =
        OrdenSacrificioProductivo.programar(galponId, loteId, ahora.plusSeconds(3600), ahora);
    when(ordenes.bloquear(orden.id())).thenReturn(Optional.of(orden));
    when(galpones.consultar(galponId))
        .thenReturn(
            Optional.of(new GalponQueryPort.GalponDatos(galponId, "G1", 1000, "EN_COSECHA")));
    when(lotes.consultarActual(galponId))
        .thenReturn(Optional.of(new Lote(loteId, galponId, 950, LocalDate.of(2026, 10, 10))));
    when(ordenes.actualizar(any())).thenReturn(false);

    assertThatThrownBy(() -> useCase.reprogramar(orden.id(), ahora.plusSeconds(7200)))
        .isInstanceOf(PrecondicionSacrificioNoCumplidaException.class);
  }
}
