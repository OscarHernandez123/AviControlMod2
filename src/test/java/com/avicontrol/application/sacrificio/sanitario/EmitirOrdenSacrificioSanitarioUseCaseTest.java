package com.avicontrol.application.sacrificio.sanitario;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.avicontrol.domain.exception.sacrificio.sanitario.DiagnosticoNoElegibleException;
import com.avicontrol.domain.port.out.modulo1.LoteQueryPort;
import com.avicontrol.domain.port.out.sacrificio.sanitario.DiagnosticoSanitarioQueryPort;
import com.avicontrol.domain.port.out.sacrificio.sanitario.OrdenSacrificioSanitarioRepositoryPort;
import com.avicontrol.domain.port.out.sanitario.aislamiento.GalponLoteAislamientoQueryPort;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EmitirOrdenSacrificioSanitarioUseCaseTest {
  @Test
  void diagnosticoNoElegibleNoCreaOrden() {
    var diagnosticos = mock(DiagnosticoSanitarioQueryPort.class);
    var ordenes = mock(OrdenSacrificioSanitarioRepositoryPort.class);
    UUID galpon = UUID.randomUUID();
    when(diagnosticos.activoPorGalpon(galpon))
        .thenReturn(
            Optional.of(
                new DiagnosticoSanitarioQueryPort.DiagnosticoDatos(
                    UUID.randomUUID(), galpon, UUID.randomUUID(), false)));
    var caso =
        new EmitirOrdenSacrificioSanitarioUseCase(
            diagnosticos,
            mock(GalponLoteAislamientoQueryPort.class),
            mock(LoteQueryPort.class),
            ordenes,
            Clock.systemUTC());
    assertThrows(DiagnosticoNoElegibleException.class, () -> caso.emitir(galpon));
    verifyNoInteractions(ordenes);
  }
}
