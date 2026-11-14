package com.avicontrol.application.sanitario.aislamiento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.avicontrol.domain.exception.sanitario.aislamiento.EstadoGalponNoAislableException;
import com.avicontrol.domain.exception.sanitario.aislamiento.Modulo1NoDisponibleException;
import com.avicontrol.domain.model.sanitario.aislamiento.DecisionAislamiento;
import com.avicontrol.domain.model.sanitario.aislamiento.EstadoSolicitudAislamiento;
import com.avicontrol.domain.model.sanitario.aislamiento.SolicitudAislamiento;
import com.avicontrol.domain.port.out.modulo1.GalponQueryPort;
import com.avicontrol.domain.port.out.modulo1.GalponUpdatePort;
import com.avicontrol.domain.port.out.sanitario.aislamiento.SolicitudAislamientoRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ValidarAislamientoUseCaseTest {
  private SolicitudAislamiento pendiente(UUID id, UUID galpon) {
    return new SolicitudAislamiento(
        id,
        galpon,
        UUID.randomUUID(),
        "Tos",
        "Decaimiento",
        8000,
        5,
        "trabajador",
        Instant.now(),
        EstadoSolicitudAislamiento.PENDIENTE_VALIDACION,
        null,
        null,
        null,
        0);
  }

  @Test
  void rechazarNoActualizaGalpon() {
    UUID id = UUID.randomUUID();
    UUID galpon = UUID.randomUUID();
    SolicitudAislamientoRepositoryPort repo = mock(SolicitudAislamientoRepositoryPort.class);
    GalponQueryPort query = mock(GalponQueryPort.class);
    GalponUpdatePort update = mock(GalponUpdatePort.class);
    when(repo.buscar(id)).thenReturn(Optional.of(pendiente(id, galpon)));
    when(query.estadoVigente(galpon)).thenReturn(Optional.of("PRODUCTIVA"));
    when(repo.resolver(any(), any())).thenReturn(true);
    var result =
        new ValidarAislamientoUseCase(repo, query, update, Clock.systemUTC())
            .validar(
                new ValidarAislamientoCommand(
                    id, DecisionAislamiento.RECHAZAR, "No procede", "vet"));
    assertEquals(EstadoSolicitudAislamiento.RECHAZADA, result.estado());
    verify(update, never()).aislarSiProductiva(any());
  }

  @Test
  void reconsultaEstadoYRechazaCambioConcurrente() {
    UUID id = UUID.randomUUID();
    UUID galpon = UUID.randomUUID();
    SolicitudAislamientoRepositoryPort repo = mock(SolicitudAislamientoRepositoryPort.class);
    GalponQueryPort query = mock(GalponQueryPort.class);
    GalponUpdatePort update = mock(GalponUpdatePort.class);
    when(repo.buscar(id)).thenReturn(Optional.of(pendiente(id, galpon)));
    when(query.estadoVigente(galpon)).thenReturn(Optional.of("PRODUCTIVA"));
    when(update.aislarSiProductiva(galpon)).thenReturn(false);
    assertThrows(
        EstadoGalponNoAislableException.class,
        () ->
            new ValidarAislamientoUseCase(repo, query, update, Clock.systemUTC())
                .validar(
                    new ValidarAislamientoCommand(
                        id, DecisionAislamiento.ACEPTAR, "Aislar", "vet")));
    verify(query).estadoVigente(galpon);
    verify(repo, never()).resolver(any(), any());
  }

  @Test
  void noResuelveSiNoPuedeConsultarGalpon() {
    UUID id = UUID.randomUUID();
    UUID galpon = UUID.randomUUID();
    SolicitudAislamientoRepositoryPort repo = mock(SolicitudAislamientoRepositoryPort.class);
    GalponQueryPort query = mock(GalponQueryPort.class);
    GalponUpdatePort update = mock(GalponUpdatePort.class);
    when(repo.buscar(id)).thenReturn(Optional.of(pendiente(id, galpon)));
    when(query.estadoVigente(galpon)).thenReturn(Optional.empty());
    assertThrows(
        Modulo1NoDisponibleException.class,
        () ->
            new ValidarAislamientoUseCase(repo, query, update, Clock.systemUTC())
                .validar(
                    new ValidarAislamientoCommand(
                        id, DecisionAislamiento.ACEPTAR, "Aislar", "vet")));
    verify(update, never()).aislarSiProductiva(any());
    verify(repo, never()).resolver(any(), any());
  }
}
