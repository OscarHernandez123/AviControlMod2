package com.avicontrol.application.sanitario.enfermedad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.avicontrol.domain.exception.sanitario.enfermedad.EnfermedadNoEncontradaException;
import com.avicontrol.domain.model.sanitario.enfermedad.Enfermedad;
import com.avicontrol.domain.model.sanitario.enfermedad.NivelRiesgo;
import com.avicontrol.domain.model.sanitario.enfermedad.TipoEnfermedad;
import com.avicontrol.domain.port.out.sanitario.enfermedad.EnfermedadRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EditarEnfermedadUseCaseTest {
  @Test
  void actualizaSoloElCatalogoYConservaOriginal() {
    UUID id = UUID.randomUUID();
    Instant now = Instant.now();
    Enfermedad original =
        new Enfermedad(
            id, "Newcastle", NivelRiesgo.ALTO, TipoEnfermedad.VIRAL, "Signos", false, 0, now, now);
    EnfermedadRepositoryPort repo = mock(EnfermedadRepositoryPort.class);
    when(repo.buscar(id)).thenReturn(Optional.of(original));
    when(repo.actualizar(any(), any())).thenReturn(true);
    var result =
        new EditarEnfermedadUseCase(repo, Clock.systemUTC())
            .editar(
                new EditarEnfermedadCommand(
                    id,
                    "Newcastle",
                    NivelRiesgo.CRITICO,
                    TipoEnfermedad.VIRAL,
                    "Otros signos",
                    true));
    assertEquals(true, result.requiereSacrificioSanitario());
    assertEquals(false, original.requiereSacrificioSanitario());
    verify(repo).actualizar(any(), any());
  }

  @Test
  void informaAusencia() {
    EnfermedadRepositoryPort repo = mock(EnfermedadRepositoryPort.class);
    UUID id = UUID.randomUUID();
    when(repo.buscar(id)).thenReturn(Optional.empty());
    assertThrows(
        EnfermedadNoEncontradaException.class,
        () ->
            new EditarEnfermedadUseCase(repo, Clock.systemUTC())
                .editar(
                    new EditarEnfermedadCommand(
                        id, "Newcastle", NivelRiesgo.ALTO, TipoEnfermedad.VIRAL, "Signos", false)));
  }
}
