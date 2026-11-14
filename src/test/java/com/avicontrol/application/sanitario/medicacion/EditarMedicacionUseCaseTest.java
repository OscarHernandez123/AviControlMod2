package com.avicontrol.application.sanitario.medicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.avicontrol.domain.exception.sanitario.medicacion.EnfermedadNoDisponibleException;
import com.avicontrol.domain.model.sanitario.medicacion.Medicacion;
import com.avicontrol.domain.port.out.sanitario.medicacion.EnfermedadQueryPort;
import com.avicontrol.domain.port.out.sanitario.medicacion.MedicacionRepositoryPort;
import com.avicontrol.domain.port.out.sanitario.medicacion.MedicamentoCatalogoQueryPort;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EditarMedicacionUseCaseTest {
  @Test
  void noActualizaSiEnfermedadNoEstaDisponible() {
    UUID id = UUID.randomUUID();
    UUID enfermedad = UUID.randomUUID();
    UUID medicamento = UUID.randomUUID();
    Instant now = Instant.now();
    Medicacion original =
        new Medicacion(id, enfermedad, medicamento, "0.5 ml", 5, "Aplicar", 0, now, now);
    MedicacionRepositoryPort repo = mock(MedicacionRepositoryPort.class);
    EnfermedadQueryPort enfermedades = mock(EnfermedadQueryPort.class);
    MedicamentoCatalogoQueryPort medicamentos = mock(MedicamentoCatalogoQueryPort.class);
    when(repo.buscar(id)).thenReturn(Optional.of(original));
    when(enfermedades.disponible(enfermedad)).thenReturn(false);
    assertThrows(
        EnfermedadNoDisponibleException.class,
        () ->
            new EditarMedicacionUseCase(repo, enfermedades, medicamentos, Clock.systemUTC())
                .editar(
                    new EditarMedicacionCommand(
                        id, enfermedad, medicamento, "1 ml", 7, "Aplicar")));
    verify(repo, never()).actualizar(any(), any());
    assertEquals(5, original.duracionDias());
  }
}
