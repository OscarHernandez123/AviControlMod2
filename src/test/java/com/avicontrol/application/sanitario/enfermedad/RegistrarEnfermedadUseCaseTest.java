package com.avicontrol.application.sanitario.enfermedad;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.avicontrol.domain.exception.sanitario.enfermedad.DatosEnfermedadInvalidosException;
import com.avicontrol.domain.model.sanitario.enfermedad.NivelRiesgo;
import com.avicontrol.domain.model.sanitario.enfermedad.TipoEnfermedad;
import com.avicontrol.domain.port.out.sanitario.enfermedad.EnfermedadRepositoryPort;
import java.time.Clock;
import org.junit.jupiter.api.Test;

class RegistrarEnfermedadUseCaseTest {
  @Test
  void noPersisteSinDecisionDeSacrificio() {
    EnfermedadRepositoryPort repo = mock(EnfermedadRepositoryPort.class);
    RegistrarEnfermedadUseCase useCase = new RegistrarEnfermedadUseCase(repo, Clock.systemUTC());
    assertThrows(
        DatosEnfermedadInvalidosException.class,
        () ->
            useCase.registrar(
                new RegistrarEnfermedadCommand(
                    "Newcastle",
                    NivelRiesgo.ALTO,
                    TipoEnfermedad.VIRAL,
                    "Signos respiratorios",
                    null)));
    verify(repo, never()).crear(any());
  }
}
