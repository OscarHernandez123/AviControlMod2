package com.avicontrol.application.sanitario.medicacion;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.avicontrol.domain.exception.sanitario.medicacion.MedicamentoNoDisponibleException;
import com.avicontrol.domain.port.out.sanitario.medicacion.EnfermedadQueryPort;
import com.avicontrol.domain.port.out.sanitario.medicacion.MedicacionRepositoryPort;
import com.avicontrol.domain.port.out.sanitario.medicacion.MedicamentoCatalogoQueryPort;
import java.time.Clock;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RegistrarMedicacionUseCaseTest {
  @Test
  void noGuardaSiMedicamentoDejoDeEstarDisponible() {
    UUID enfermedad = UUID.randomUUID();
    UUID medicamento = UUID.randomUUID();
    MedicacionRepositoryPort repo = mock(MedicacionRepositoryPort.class);
    EnfermedadQueryPort enfermedades = mock(EnfermedadQueryPort.class);
    MedicamentoCatalogoQueryPort medicamentos = mock(MedicamentoCatalogoQueryPort.class);
    when(enfermedades.disponible(enfermedad)).thenReturn(true);
    when(medicamentos.disponible(medicamento)).thenReturn(false);
    assertThrows(
        MedicamentoNoDisponibleException.class,
        () ->
            new RegistrarMedicacionUseCase(repo, enfermedades, medicamentos, Clock.systemUTC())
                .registrar(
                    new RegistrarMedicacionCommand(
                        enfermedad, medicamento, "0.5 ml", 5, "Aplicar")));
    verify(repo, never()).crear(any());
  }
}
