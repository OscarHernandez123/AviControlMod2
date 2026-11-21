package com.avicontrol.application.galpon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.avicontrol.domain.model.lote.Lote;
import com.avicontrol.domain.port.out.modulo1.GalponQueryPort;
import com.avicontrol.domain.port.out.modulo1.LoteQueryPort;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsultarGalponUseCaseTest {
  @Test
  void conservaDatosYReportaFechaFuturaSinEdad() {
    var galpones = mock(GalponQueryPort.class);
    var lotes = mock(LoteQueryPort.class);
    UUID id = UUID.randomUUID();
    when(galpones.consultar(id))
        .thenReturn(Optional.of(new GalponQueryPort.GalponDatos(id, "G1", 9000, "PRODUCTIVO")));
    when(lotes.consultarActual(id))
        .thenReturn(Optional.of(new Lote(UUID.randomUUID(), id, 8000, LocalDate.of(2024, 3, 17))));
    var caso =
        new ConsultarGalponUseCase(
            galpones,
            lotes,
            Clock.fixed(Instant.parse("2024-03-16T12:00:00Z"), ZoneId.of("America/Bogota")));
    var detalle = caso.consultar(id);
    assertEquals("G1", detalle.nombre());
    assertEquals(8000, detalle.loteActual().poblacionActual());
    assertNull(detalle.loteActual().edad());
    assertEquals("La fecha de ingreso del lote es inconsistente", detalle.loteActual().mensaje());
  }
}
