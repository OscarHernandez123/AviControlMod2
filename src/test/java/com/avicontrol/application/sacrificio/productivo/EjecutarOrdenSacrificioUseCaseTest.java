package com.avicontrol.application.sacrificio.productivo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.avicontrol.domain.exception.sacrificio.productivo.PrecondicionSacrificioNoCumplidaException;
import com.avicontrol.domain.model.lote.Lote;
import com.avicontrol.domain.model.sacrificio.productivo.EstadoOrdenSacrificio;
import com.avicontrol.domain.model.sacrificio.productivo.OrdenSacrificioProductivo;
import com.avicontrol.domain.port.out.modulo1.GalponQueryPort;
import com.avicontrol.domain.port.out.modulo1.GalponUpdatePort;
import com.avicontrol.domain.port.out.modulo1.LoteQueryPort;
import com.avicontrol.domain.port.out.modulo1.LoteUpdatePort;
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
class EjecutarOrdenSacrificioUseCaseTest {
  @Mock OrdenSacrificioProductivoRepositoryPort ordenes;
  @Mock GalponQueryPort galpones;
  @Mock LoteQueryPort lotes;
  @Mock GalponUpdatePort cambiosGalpon;
  @Mock LoteUpdatePort cambiosLote;

  private final Clock clock =
      Clock.fixed(Instant.parse("2026-11-20T15:00:00Z"), ZoneId.of("America/Bogota"));

  @Test
  void ejecutaOrdenVencidaYActualizaGalponYLote() {
    EjecutarOrdenSacrificioUseCase useCase =
        new EjecutarOrdenSacrificioUseCase(
            ordenes, galpones, lotes, cambiosGalpon, cambiosLote, clock);
    UUID galponId = UUID.randomUUID();
    UUID loteId = UUID.randomUUID();
    Instant ahora = Instant.now(clock);
    OrdenSacrificioProductivo orden =
        OrdenSacrificioProductivo.programar(
            galponId, loteId, ahora.minusSeconds(60), ahora.minusSeconds(3600));

    when(ordenes.bloquear(orden.id())).thenReturn(Optional.of(orden));
    when(galpones.consultar(galponId))
        .thenReturn(
            Optional.of(new GalponQueryPort.GalponDatos(galponId, "G1", 1000, "EN_COSECHA")));
    when(lotes.consultarActual(galponId))
        .thenReturn(Optional.of(new Lote(loteId, galponId, 950, LocalDate.of(2026, 10, 10))));
    when(cambiosGalpon.vaciarSiEnCosecha(galponId, loteId)).thenReturn(true);
    when(cambiosLote.finalizarAlojamiento(galponId, loteId)).thenReturn(true);
    when(ordenes.actualizar(any())).thenReturn(true);

    assertThat(useCase.ejecutar(orden.id())).isTrue();
    verify(cambiosLote).finalizarAlojamiento(galponId, loteId);
    verify(ordenes)
        .actualizar(
            argThat(
                updated ->
                    updated.id().equals(orden.id())
                        && updated.estado() == EstadoOrdenSacrificio.EJECUTADA
                        && ahora.equals(updated.fechaEjecucion())));
  }

  @Test
  void retornaFalseSiNoVencidaYLanzaSiFallaActualizacionGalpon() {
    EjecutarOrdenSacrificioUseCase useCase =
        new EjecutarOrdenSacrificioUseCase(
            ordenes, galpones, lotes, cambiosGalpon, cambiosLote, clock);
    UUID galponId = UUID.randomUUID();
    UUID loteId = UUID.randomUUID();
    Instant ahora = Instant.now(clock);

    OrdenSacrificioProductivo futura =
        OrdenSacrificioProductivo.programar(galponId, loteId, ahora.plusSeconds(3600), ahora);
    when(ordenes.bloquear(futura.id())).thenReturn(Optional.of(futura));
    assertThat(useCase.ejecutar(futura.id())).isFalse();

    OrdenSacrificioProductivo vencida =
        OrdenSacrificioProductivo.programar(
            galponId, loteId, ahora.minusSeconds(60), ahora.minusSeconds(3600));
    when(ordenes.bloquear(vencida.id())).thenReturn(Optional.of(vencida));
    when(galpones.consultar(galponId))
        .thenReturn(
            Optional.of(new GalponQueryPort.GalponDatos(galponId, "G1", 1000, "EN_COSECHA")));
    when(lotes.consultarActual(galponId))
        .thenReturn(Optional.of(new Lote(loteId, galponId, 950, LocalDate.of(2026, 10, 10))));
    when(cambiosGalpon.vaciarSiEnCosecha(galponId, loteId)).thenReturn(false);

    assertThatThrownBy(() -> useCase.ejecutar(vencida.id()))
        .isInstanceOf(PrecondicionSacrificioNoCumplidaException.class);
    verifyNoInteractions(cambiosLote);
    verify(ordenes, never()).actualizar(any());
  }
}
