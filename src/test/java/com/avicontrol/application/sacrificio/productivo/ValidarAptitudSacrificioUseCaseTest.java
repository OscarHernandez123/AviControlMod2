package com.avicontrol.application.sacrificio.productivo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.avicontrol.domain.exception.galpon.InformacionGalponNoDisponibleException;
import com.avicontrol.domain.model.lote.Lote;
import com.avicontrol.domain.port.out.modulo1.GalponQueryPort;
import com.avicontrol.domain.port.out.modulo1.GalponUpdatePort;
import com.avicontrol.domain.port.out.modulo1.LoteQueryPort;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ValidarAptitudSacrificioUseCaseTest {
  private final GalponQueryPort galpones = mock(GalponQueryPort.class);
  private final LoteQueryPort lotes = mock(LoteQueryPort.class);
  private final GalponUpdatePort cambios = mock(GalponUpdatePort.class);
  private final Clock clock =
      Clock.fixed(Instant.parse("2024-03-16T12:00:00Z"), ZoneId.of("America/Bogota"));
  private final ValidarAptitudSacrificioUseCase caso =
      new ValidarAptitudSacrificioUseCase(galpones, lotes, cambios, clock);

  @Test
  void actualizaSoloCuandoCumpleEdadYEstado() {
    UUID id = UUID.randomUUID();
    UUID loteId = UUID.randomUUID();
    when(galpones.consultar(id))
        .thenReturn(Optional.of(new GalponQueryPort.GalponDatos(id, "G", 9000, "PRODUCTIVO")));
    when(lotes.consultarActual(id))
        .thenReturn(Optional.of(new Lote(loteId, id, 8000, LocalDate.now(clock).minusDays(44))));
    when(cambios.pasarACosechaSiProductiva(id, loteId)).thenReturn(true);
    assertEquals(
        "EN_COSECHA",
        caso.validar(new ValidarAptitudSacrificioCommand(id)).estadoGalponResultante());
    verify(cambios).pasarACosechaSiProductiva(id, loteId);
  }

  @Test
  void dependenciaFallidaNoActualiza() {
    UUID id = UUID.randomUUID();
    when(galpones.consultar(id)).thenThrow(new InformacionGalponNoDisponibleException());
    assertThrows(
        InformacionGalponNoDisponibleException.class,
        () -> caso.validar(new ValidarAptitudSacrificioCommand(id)));
    verifyNoInteractions(lotes, cambios);
  }

  @Test
  void noAptoNoActualiza() {
    UUID id = UUID.randomUUID();
    when(galpones.consultar(id))
        .thenReturn(Optional.of(new GalponQueryPort.GalponDatos(id, "G", 9000, "MANTENIMIENTO")));
    when(lotes.consultarActual(id))
        .thenReturn(
            Optional.of(new Lote(UUID.randomUUID(), id, 8000, LocalDate.now(clock).minusDays(43))));
    assertFalse(caso.validar(new ValidarAptitudSacrificioCommand(id)).apto());
    verifyNoInteractions(cambios);
  }
}
