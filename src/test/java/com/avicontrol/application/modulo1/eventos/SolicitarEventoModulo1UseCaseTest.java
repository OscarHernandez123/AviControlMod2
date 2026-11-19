package com.avicontrol.application.modulo1.eventos;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.avicontrol.application.modulo1.eventos.SolicitarEventoModulo1UseCase.*;
import com.avicontrol.domain.model.lote.Lote;
import com.avicontrol.domain.port.out.modulo1.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SolicitarEventoModulo1UseCaseTest {
  private final EventosModulo1Port eventos = mock(EventosModulo1Port.class);
  private final GalponQueryPort galpones = mock(GalponQueryPort.class);
  private final LoteQueryPort lotes = mock(LoteQueryPort.class);
  private final UUID alerta = UUID.randomUUID(),
      galpon = UUID.randomUUID(),
      lote = UUID.randomUUID();
  private final OffsetDateTime fecha = OffsetDateTime.parse("2026-11-19T10:00:00-05:00");
  private final SolicitarEventoModulo1UseCase caso =
      new SolicitarEventoModulo1UseCase(
          eventos,
          galpones,
          lotes,
          Clock.fixed(fecha.plusMinutes(1).toInstant(), ZoneId.of("America/Bogota")));

  @BeforeEach
  void preparar() {
    when(eventos.repetido(any())).thenReturn(Optional.empty());
    when(eventos.encolar(any())).thenReturn(new EventosModulo1Port.Entrega(alerta, "PENDIENTE", 0));
    estado("PRODUCTIVO");
    when(lotes.consultarActual(galpon))
        .thenReturn(Optional.of(new Lote(lote, galpon, 100, LocalDate.of(2026, 11, 1))));
  }

  private void estado(String estado) {
    when(galpones.consultar(galpon))
        .thenReturn(Optional.of(new GalponQueryPort.GalponDatos(galpon, "Norte", 100, estado)));
  }

  private Solicitud solicitud(OffsetDateTime fecha, Integer cantidad, String accion) {
    return new Solicitud(
        alerta, galpon, lote, fecha, cantidad, accion, "Sospecha", "Signos respiratorios", null);
  }

  private EventosModulo1Port.Evento enviado() {
    var captor = ArgumentCaptor.forClass(EventosModulo1Port.Evento.class);
    verify(eventos).encolar(captor.capture());
    return captor.getValue();
  }

  @Test
  void mortalidadUsaCamposYTiposDelContrato() {
    caso.solicitar(Tipo.MORTALIDAD, solicitud(fecha, 3, null));
    var e = enviado();
    assertEquals("avicontrol.mortalidad", e.topic());
    assertEquals("MORTALIDAD_REGISTRADA", e.eventType());
    assertEquals(galpon, e.galponId());
    assertEquals(
        Set.of("alertaId", "galponId", "loteId", "fechaHoraEvento", "cantidadMuertos"),
        e.payload().keySet());
    assertEquals(3, e.payload().get("cantidadMuertos"));
    assertEquals(fecha.toString(), e.payload().get("fechaHoraEvento"));
  }

  @Test
  void aislamientoOmiteOpcionalesNulos() {
    caso.solicitar(Tipo.SANITARIA, solicitud(fecha, null, "AISLAMIENTO"));
    var e = enviado();
    assertEquals("ALERTA_SANITARIA_EMITIDA", e.eventType());
    assertEquals("avicontrol.alerta-sanitaria", e.topic());
    assertFalse(e.payload().containsKey("gravedad"));
    assertTrue(e.payload().containsKey("fechaHora"));
    assertEquals("AISLAMIENTO", e.payload().get("accion"));
  }

  @Test
  void reanudacionNoEnviaDatosDeEnfermedad() {
    estado("AISLAMIENTO");
    caso.solicitar(Tipo.SANITARIA, solicitud(fecha, null, "REANUDACION"));
    assertFalse(enviado().payload().containsKey("tipoEnfermedad"));
  }

  @Test
  void vaciadoSoloEnCosecha() {
    assertThrows(
        IllegalStateException.class,
        () -> caso.solicitar(Tipo.VACIADO, solicitud(fecha, null, null)));
    estado("EN_COSECHA");
    caso.solicitar(Tipo.VACIADO, solicitud(fecha, null, null));
    assertEquals("VACIADO_SANITARIO_SOLICITADO", enviado().eventType());
    assertEquals("avicontrol.vaciado-sanitario", enviado().topic());
  }

  @Test
  void rechazaFechasInvalidasYExcesoDePoblacion() {
    assertThrows(
        IllegalArgumentException.class,
        () -> caso.solicitar(Tipo.MORTALIDAD, solicitud(fecha.minusDays(1), 2, null)));
    assertThrows(
        IllegalArgumentException.class,
        () -> caso.solicitar(Tipo.MORTALIDAD, solicitud(fecha.plusHours(1), 2, null)));
    assertThrows(
        IllegalStateException.class,
        () -> caso.solicitar(Tipo.MORTALIDAD, solicitud(fecha, 101, null)));
    verify(eventos, never()).encolar(any());
  }

  @Test
  void reintentoExistenteNoDependeDelEstadoRemotoActual() {
    var entrega = new EventosModulo1Port.Entrega(alerta, "PUBLICADO", 1);
    when(eventos.repetido(any())).thenReturn(Optional.of(entrega));
    assertEquals(entrega, caso.solicitar(Tipo.MORTALIDAD, solicitud(fecha.minusDays(1), 3, null)));
    verifyNoInteractions(galpones, lotes);
    verify(eventos, never()).encolar(any());
  }

  @Test
  void loteAjenoSeRechaza() {
    var s = new Solicitud(alerta, galpon, UUID.randomUUID(), fecha, 1, null, null, null, null);
    assertThrows(IllegalStateException.class, () -> caso.solicitar(Tipo.MORTALIDAD, s));
  }

  @Test
  void normalizaOffsetDeEntradaSinCambiarInstante() {
    caso.solicitar(
        Tipo.MORTALIDAD, solicitud(fecha.withOffsetSameInstant(ZoneOffset.UTC), 3, null));
    assertEquals(fecha.toString(), enviado().payload().get("fechaHoraEvento"));
  }
}
