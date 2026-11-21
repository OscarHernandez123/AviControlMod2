package com.avicontrol.application.galpon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.avicontrol.domain.exception.galpon.GalponNoEncontradoException;
import com.avicontrol.domain.model.lote.Lote;
import com.avicontrol.domain.port.out.modulo1.GalponQueryPort;
import com.avicontrol.domain.port.out.modulo1.LoteQueryPort;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsultarEdadPorGalponUseCaseTest {
  private final GalponQueryPort galpones = mock(GalponQueryPort.class);
  private final LoteQueryPort lotes = mock(LoteQueryPort.class);
  private final Clock clock =
      Clock.fixed(Instant.parse("2024-03-16T12:00:00Z"), ZoneId.of("America/Bogota"));
  private final ConsultarEdadPorGalponUseCase caso =
      new ConsultarEdadPorGalponUseCase(galpones, lotes, clock);

  @Test
  void consultaLoteYDevuelveAusenciaSinInventarEdad() {
    UUID id = UUID.randomUUID();
    UUID loteId = UUID.randomUUID();
    when(galpones.consultar(id))
        .thenReturn(Optional.of(new GalponQueryPort.GalponDatos(id, "G1", 9000, "PRODUCTIVO")));
    when(lotes.consultarActual(id))
        .thenReturn(Optional.of(new Lote(loteId, id, 8000, java.time.LocalDate.of(2024, 2, 29))))
        .thenReturn(Optional.empty());
    assertEquals(17, caso.consultar(id).edad().diasTotales());
    assertNull(caso.consultar(id).edad());
  }

  @Test
  void noConfundeGalponAusenteConGalponSinLote() {
    assertThrows(GalponNoEncontradoException.class, () -> caso.consultar(UUID.randomUUID()));
  }
}
