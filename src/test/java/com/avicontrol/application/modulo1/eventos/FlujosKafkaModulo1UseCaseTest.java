package com.avicontrol.application.modulo1.eventos;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.avicontrol.application.mortalidad.RegistrarMortalidadCommand;
import com.avicontrol.application.sanitario.aislamiento.ValidarAislamientoCommand;
import com.avicontrol.domain.model.lote.Lote;
import com.avicontrol.domain.model.mortalidad.CausaMortalidad;
import com.avicontrol.domain.model.sanitario.aislamiento.*;
import com.avicontrol.domain.port.out.modulo1.*;
import com.avicontrol.domain.port.out.sanitario.aislamiento.SolicitudAislamientoRepositoryPort;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class FlujosKafkaModulo1UseCaseTest {
  private final OperacionesModulo1Port operaciones = mock(OperacionesModulo1Port.class);
  private final EventosModulo1Port entregas = mock(EventosModulo1Port.class);
  private final SolicitarEventoModulo1UseCase eventos = mock(SolicitarEventoModulo1UseCase.class);
  private final LoteQueryPort lotes = mock(LoteQueryPort.class);
  private final GalponQueryPort galpones = mock(GalponQueryPort.class);
  private final SolicitudAislamientoRepositoryPort solicitudes =
      mock(SolicitudAislamientoRepositoryPort.class);
  private final UUID id = UUID.randomUUID(), galpon = UUID.randomUUID(), lote = UUID.randomUUID();
  private final Instant ahora = Instant.parse("2026-11-19T15:00:00Z");
  private final FlujosKafkaModulo1UseCase caso =
      new FlujosKafkaModulo1UseCase(
          operaciones,
          entregas,
          eventos,
          lotes,
          galpones,
          solicitudes,
          Clock.fixed(ahora, ZoneId.of("America/Bogota")));

  @Test
  void mortalidadPreservaDatosDelFormularioYEncolaSinAfirmarPoblacionResultante() {
    when(lotes.consultarActual(galpon))
        .thenReturn(Optional.of(new Lote(lote, galpon, 100, LocalDate.of(2026, 10, 1))));
    when(entregas.consultar(id)).thenReturn(new EventosModulo1Port.Entrega(id, "PENDIENTE", 0));
    var result =
        caso.mortalidad(
            new RegistrarMortalidadCommand(
                galpon,
                ahora,
                2,
                CausaMortalidad.DESCONOCIDA,
                "Observación clínica",
                "trabajador",
                id));
    var capture = ArgumentCaptor.forClass(OperacionesModulo1Port.Operacion.class);
    verify(operaciones).guardar(capture.capture());
    assertEquals("Observación clínica", capture.getValue().datos().get("observaciones"));
    assertEquals("trabajador", capture.getValue().datos().get("responsable"));
    assertEquals("PENDIENTE", result.integracion().estado());
    assertNull(result.estadoGalponResultante());
    verify(eventos).solicitar(eq(SolicitarEventoModulo1UseCase.Tipo.MORTALIDAD), any());
  }

  @Test
  void reintentarUnCambioAutomaticoNoVuelveAEncolar() {
    when(operaciones.buscar(id))
        .thenReturn(
            Optional.of(
                new OperacionesModulo1Port.Operacion(id, "VACIADO", galpon, lote, Map.of())));
    assertFalse(caso.solicitarCambio(id, "VACIADO", galpon, lote));
    verifyNoInteractions(eventos);
  }

  @Test
  void rechazarAislamientoResuelveSoloLaDecisionVeterinaria() {
    var s =
        new SolicitudAislamiento(
            id,
            galpon,
            lote,
            "Motivo",
            "Signos",
            100,
            20,
            "trabajador",
            ahora.minusSeconds(10),
            EstadoSolicitudAislamiento.PENDIENTE_VALIDACION,
            null,
            null,
            null,
            0);
    when(solicitudes.buscar(id)).thenReturn(Optional.of(s));
    when(solicitudes.resolver(eq(s), any())).thenReturn(true);
    var result =
        caso.aislamiento(
            new ValidarAislamientoCommand(id, DecisionAislamiento.RECHAZAR, "No procede", "vet"));
    assertEquals("RECHAZADA", result.estado());
    assertNull(result.integracion());
    verifyNoInteractions(eventos);
    verify(operaciones, never()).guardar(any());
  }

  @Test
  void aceptarAislamientoEncolaConIdentidadDeLaSolicitud() {
    var s =
        new SolicitudAislamiento(
            id,
            galpon,
            lote,
            "Sospecha",
            "Signos",
            100,
            20,
            "trabajador",
            ahora.minusSeconds(10),
            EstadoSolicitudAislamiento.PENDIENTE_VALIDACION,
            null,
            null,
            null,
            0);
    when(solicitudes.buscar(id)).thenReturn(Optional.of(s));
    when(solicitudes.resolver(eq(s), any())).thenReturn(true);
    when(entregas.consultar(id)).thenReturn(new EventosModulo1Port.Entrega(id, "PENDIENTE", 0));
    var result =
        caso.aislamiento(
            new ValidarAislamientoCommand(id, DecisionAislamiento.ACEPTAR, "Aislar", "vet"));
    assertEquals("ACEPTADA", result.estado());
    assertNull(result.estadoGalponResultante());
    var captor = ArgumentCaptor.forClass(SolicitarEventoModulo1UseCase.Solicitud.class);
    verify(eventos).solicitar(eq(SolicitarEventoModulo1UseCase.Tipo.SANITARIA), captor.capture());
    assertEquals(id, captor.getValue().alertaId());
    assertEquals("Sospecha", captor.getValue().tipoEnfermedad());
  }
}
