package com.avicontrol.application.sacrificio.productivo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.avicontrol.domain.exception.galpon.GalponNoEncontradoException;
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
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProgramarOrdenSacrificioUseCaseTest {
  @Mock GalponQueryPort galpones;
  @Mock LoteQueryPort lotes;
  @Mock OrdenSacrificioProductivoRepositoryPort ordenes;

  private final Clock clock =
      Clock.fixed(Instant.parse("2026-11-20T15:00:00Z"), ZoneId.of("America/Bogota"));

  @Test
  void programaOrdenCuandoGalponEstaEnCosechaYNoExisteOrdenPrevia() {
    ProgramarOrdenSacrificioUseCase useCase =
        new ProgramarOrdenSacrificioUseCase(galpones, lotes, ordenes, clock);
    UUID galponId = UUID.randomUUID();
    UUID loteId = UUID.randomUUID();
    Instant fecha = Instant.now(clock).plusSeconds(7200);

    when(galpones.consultar(galponId))
        .thenReturn(
            Optional.of(new GalponQueryPort.GalponDatos(galponId, "G1", 1000, "EN_COSECHA")));
    when(lotes.consultarActual(galponId))
        .thenReturn(Optional.of(new Lote(loteId, galponId, 950, LocalDate.of(2026, 10, 10))));
    when(ordenes.existePorGalponLote(galponId, loteId)).thenReturn(false);

    var result = useCase.programar(galponId, fecha);

    assertThat(result.estado()).isEqualTo("PROGRAMADA");
    verify(ordenes).crear(any(OrdenSacrificioProductivo.class));
  }

  @Test
  void rechazaSiGalponNoExisteOOrdenDuplicada() {
    ProgramarOrdenSacrificioUseCase useCase =
        new ProgramarOrdenSacrificioUseCase(galpones, lotes, ordenes, clock);
    UUID galponId = UUID.randomUUID();
    UUID loteId = UUID.randomUUID();
    Instant fecha = Instant.now(clock).plusSeconds(7200);

    when(galpones.consultar(galponId)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> useCase.programar(galponId, fecha))
        .isInstanceOf(GalponNoEncontradoException.class);

    when(galpones.consultar(galponId))
        .thenReturn(
            Optional.of(new GalponQueryPort.GalponDatos(galponId, "G1", 1000, "EN_COSECHA")));
    when(lotes.consultarActual(galponId))
        .thenReturn(Optional.of(new Lote(loteId, galponId, 950, LocalDate.of(2026, 10, 10))));
    when(ordenes.existePorGalponLote(galponId, loteId)).thenReturn(true);

    assertThatThrownBy(() -> useCase.programar(galponId, fecha))
        .isInstanceOf(PrecondicionSacrificioNoCumplidaException.class);
  }
}
