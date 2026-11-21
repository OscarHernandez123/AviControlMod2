package com.avicontrol.application.sacrificio.productivo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.avicontrol.domain.exception.sacrificio.productivo.PrecondicionSacrificioNoCumplidaException;
import com.avicontrol.domain.model.sacrificio.productivo.OrdenSacrificioProductivo;
import com.avicontrol.domain.port.out.sacrificio.productivo.OrdenSacrificioProductivoRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CancelarOrdenSacrificioUseCaseTest {
  @Mock OrdenSacrificioProductivoRepositoryPort ordenes;

  private final Clock clock =
      Clock.fixed(Instant.parse("2026-11-20T15:00:00Z"), ZoneId.of("America/Bogota"));

  @Test
  void cancelaOrdenYRechazaConflictoDeVersionOInexistencia() {
    CancelarOrdenSacrificioUseCase useCase = new CancelarOrdenSacrificioUseCase(ordenes, clock);
    UUID idAusente = UUID.randomUUID();
    when(ordenes.bloquear(idAusente)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> useCase.cancelar(idAusente))
        .isInstanceOf(NoSuchElementException.class);

    Instant ahora = Instant.now(clock);
    OrdenSacrificioProductivo orden =
        OrdenSacrificioProductivo.programar(
            UUID.randomUUID(), UUID.randomUUID(), ahora.plusSeconds(3600), ahora);
    when(ordenes.bloquear(orden.id())).thenReturn(Optional.of(orden));
    when(ordenes.actualizar(any())).thenReturn(true);

    var cancelada = useCase.cancelar(orden.id());
    assertThat(cancelada.estado()).isEqualTo("CANCELADA");

    when(ordenes.actualizar(any())).thenReturn(false);
    assertThatThrownBy(() -> useCase.cancelar(orden.id()))
        .isInstanceOf(PrecondicionSacrificioNoCumplidaException.class);
  }
}
