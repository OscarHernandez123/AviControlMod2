package com.avicontrol.application.sanitario.aislamiento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.avicontrol.domain.exception.sanitario.aislamiento.EstadoGalponNoAislableException;
import com.avicontrol.domain.exception.sanitario.aislamiento.LoteActivoNoEncontradoException;
import com.avicontrol.domain.model.sanitario.aislamiento.GalponLoteAislamiento;
import com.avicontrol.domain.port.out.sanitario.aislamiento.GalponLoteAislamientoQueryPort;
import com.avicontrol.domain.port.out.sanitario.aislamiento.SolicitudAislamientoRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CrearSolicitudAislamientoUseCaseTest {
  private final Clock clock = Clock.fixed(Instant.parse("2026-11-06T12:00:00Z"), ZoneOffset.UTC);

  @Test
  void capturaEdadInclusivaYNoActualizaGalpon() {
    UUID galpon = UUID.randomUUID();
    SolicitudAislamientoRepositoryPort repo = mock(SolicitudAislamientoRepositoryPort.class);
    GalponLoteAislamientoQueryPort query = mock(GalponLoteAislamientoQueryPort.class);
    when(query.consultar(galpon))
        .thenReturn(
            Optional.of(
                new GalponLoteAislamiento(
                    galpon, UUID.randomUUID(), "PRODUCTIVA", 8000, LocalDate.of(2026, 11, 1))));
    var result =
        new CrearSolicitudAislamientoUseCase(repo, query, clock)
            .crear(
                new CrearSolicitudAislamientoCommand(galpon, "Tos", "Decaimiento", "trabajador"));
    assertEquals(6, result.edadDiasReportada());
    assertEquals(8000, result.poblacionVivaReportada());
    verify(repo).crear(any());
  }

  @Test
  void rechazaGalponNoProductivoSinGuardar() {
    UUID galpon = UUID.randomUUID();
    SolicitudAislamientoRepositoryPort repo = mock(SolicitudAislamientoRepositoryPort.class);
    GalponLoteAislamientoQueryPort query = mock(GalponLoteAislamientoQueryPort.class);
    when(query.consultar(galpon))
        .thenReturn(
            Optional.of(
                new GalponLoteAislamiento(
                    galpon, UUID.randomUUID(), "AISLAMIENTO", 8000, LocalDate.of(2026, 11, 1))));
    assertThrows(
        EstadoGalponNoAislableException.class,
        () ->
            new CrearSolicitudAislamientoUseCase(repo, query, clock)
                .crear(
                    new CrearSolicitudAislamientoCommand(
                        galpon, "Tos", "Decaimiento", "trabajador")));
    verify(repo, never()).crear(any());
  }

  @Test
  void rechazaSinLoteActivoOSinAves() {
    UUID galpon = UUID.randomUUID();
    SolicitudAislamientoRepositoryPort repo = mock(SolicitudAislamientoRepositoryPort.class);
    GalponLoteAislamientoQueryPort query = mock(GalponLoteAislamientoQueryPort.class);
    var caso = new CrearSolicitudAislamientoUseCase(repo, query, clock);
    var command = new CrearSolicitudAislamientoCommand(galpon, "Tos", "Decaimiento", "trabajador");
    when(query.consultar(galpon))
        .thenReturn(
            Optional.of(
                new GalponLoteAislamiento(
                    galpon, null, "PRODUCTIVA", 8000, LocalDate.of(2026, 11, 1))));
    assertThrows(LoteActivoNoEncontradoException.class, () -> caso.crear(command));
    when(query.consultar(galpon))
        .thenReturn(
            Optional.of(
                new GalponLoteAislamiento(
                    galpon, UUID.randomUUID(), "PRODUCTIVA", 0, LocalDate.of(2026, 11, 1))));
    assertThrows(EstadoGalponNoAislableException.class, () -> caso.crear(command));
    verify(repo, never()).crear(any());
  }
}
