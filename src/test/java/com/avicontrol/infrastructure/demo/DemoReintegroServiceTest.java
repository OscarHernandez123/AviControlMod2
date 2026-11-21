package com.avicontrol.infrastructure.demo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.avicontrol.domain.model.sanitario.aislamiento.GalponLoteAislamiento;
import com.avicontrol.domain.model.sanitario.diagnostico.DiagnosticoGalpon;
import com.avicontrol.domain.model.sanitario.diagnostico.EstadoDiagnostico;
import com.avicontrol.domain.port.out.sanitario.aislamiento.GalponLoteAislamientoQueryPort;
import com.avicontrol.domain.port.out.sanitario.diagnostico.DiagnosticoGalponRepositoryPort;
import com.avicontrol.infrastructure.adapter.out.integration.modulo1.GalponReintegroLocalAdapter;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

class DemoReintegroServiceTest {
  final DiagnosticoGalponRepositoryPort repository = mock(DiagnosticoGalponRepositoryPort.class);
  final GalponLoteAislamientoQueryPort galpones = mock(GalponLoteAislamientoQueryPort.class);
  final GalponReintegroLocalAdapter reintegro = mock(GalponReintegroLocalAdapter.class);
  final JdbcTemplate jdbc = mock(JdbcTemplate.class);
  final Clock clock =
      Clock.fixed(Instant.parse("2026-11-21T16:00:00Z"), ZoneId.of("America/Bogota"));
  final DemoReintegroService service =
      new DemoReintegroService(repository, galpones, reintegro, jdbc, clock);
  final UUID id = UUID.randomUUID(), galpon = UUID.randomUUID(), lote = UUID.randomUUID();
  DiagnosticoGalpon diagnosis;

  @BeforeEach
  void setup() {
    diagnosis =
        new DiagnosticoGalpon(
            id,
            galpon,
            lote,
            UUID.randomUUID(),
            "DEMO",
            false,
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Simulación",
            1,
            "Tratamiento DEMO",
            "Observación original",
            LocalDate.of(2026, 11, 21),
            LocalDate.of(2026, 11, 22),
            EstadoDiagnostico.ACTIVO,
            null,
            0);
    when(jdbc.queryForObject("SELECT current_database()", String.class))
        .thenReturn("avicontrol_modulo2_demo");
    when(repository.bloquear(id)).thenReturn(Optional.of(diagnosis));
    when(galpones.consultar(galpon))
        .thenReturn(
            Optional.of(
                new GalponLoteAislamiento(
                    galpon, lote, "AISLAMIENTO", 1000, LocalDate.of(2026, 10, 7))));
    when(reintegro.reintegrarSiAislado(galpon, lote)).thenReturn(true);
    when(repository.resolver(eq(diagnosis), any())).thenReturn(true);
    when(jdbc.update(anyString(), anyString(), eq(id), anyLong())).thenReturn(1);
    when(repository.buscar(id))
        .thenReturn(
            Optional.of(diagnosis.resolver(EstadoDiagnostico.REINTEGRADO, LocalDate.now(clock))));
  }

  @Test
  void rechazaBasePrincipalAntesDeLeerOModificarDiagnosticos() {
    when(jdbc.queryForObject("SELECT current_database()", String.class))
        .thenReturn("avicontrol_modulo2");
    var error =
        assertThrows(
            ResponseStatusException.class,
            () -> service.adelantar(id, "Prueba DEMO", "veterinario"));
    assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    verifyNoInteractions(repository, galpones, reintegro);
  }

  @Test
  void reintegraSoloElDiagnosticoConservandoSuPrescripcionYFechaPrevista() {
    var result = service.adelantar(id, "Prueba M3", "veterinario");
    var saved = ArgumentCaptor.forClass(DiagnosticoGalpon.class);
    verify(repository).resolver(eq(diagnosis), saved.capture());
    assertEquals(EstadoDiagnostico.REINTEGRADO, result.estado());
    assertEquals(diagnosis.fechaReintegro(), saved.getValue().fechaReintegro());
    assertEquals(diagnosis.medicamentoId(), saved.getValue().medicamentoId());
    assertEquals(diagnosis.observaciones(), saved.getValue().observaciones());
    assertEquals(LocalDate.now(clock), saved.getValue().fechaResolucion());
    verify(jdbc)
        .update(
            contains("observaciones = observaciones ||"),
            contains("DEMO: reintegro anticipado por veterinario"),
            eq(id),
            eq(1L));
  }

  @Test
  void unReintentoNoVuelveACambiarElGalpon() {
    when(repository.bloquear(id))
        .thenReturn(
            Optional.of(diagnosis.resolver(EstadoDiagnostico.REINTEGRADO, LocalDate.now(clock))));
    assertEquals(
        EstadoDiagnostico.REINTEGRADO, service.adelantar(id, "Reintento", "veterinario").estado());
    verifyNoInteractions(galpones, reintegro);
    verify(repository, never()).resolver(any(), any());
  }

  @Test
  void rechazaOtroLoteSinModificarEstados() {
    when(galpones.consultar(galpon))
        .thenReturn(
            Optional.of(
                new GalponLoteAislamiento(
                    galpon, UUID.randomUUID(), "AISLAMIENTO", 1000, LocalDate.now(clock))));
    assertThrows(
        ResponseStatusException.class, () -> service.adelantar(id, "Prueba", "veterinario"));
    verifyNoInteractions(reintegro);
    verify(repository, never()).resolver(any(), any());
  }

  @Test
  void noAdelantaSiExisteOtroDiagnosticoActivo() {
    when(repository.existeOtroActivo(galpon, id)).thenReturn(true);
    assertThrows(
        ResponseStatusException.class, () -> service.adelantar(id, "Prueba", "veterinario"));
    verifyNoInteractions(reintegro);
  }

  @Test
  void fallaSiElDiagnosticoCambioConcurrentemente() {
    when(repository.resolver(eq(diagnosis), any())).thenReturn(false);
    assertThrows(
        ResponseStatusException.class, () -> service.adelantar(id, "Prueba", "veterinario"));
    verify(jdbc, never()).update(anyString(), anyString(), eq(id), anyLong());
  }
}
