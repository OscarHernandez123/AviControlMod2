package com.avicontrol.application.medicamento;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.avicontrol.domain.exception.medicamento.TratamientoNoActivoException;
import com.avicontrol.domain.model.medicamento.UnidadConsumoMedicamento;
import com.avicontrol.domain.model.sanitario.diagnostico.DiagnosticoGalpon;
import com.avicontrol.domain.model.sanitario.diagnostico.EstadoDiagnostico;
import com.avicontrol.domain.port.out.medicamento.ConsumoMedicamentoRepositoryPort;
import com.avicontrol.domain.port.out.medicamento.DiagnosticoQueryPort;
import com.avicontrol.domain.port.out.medicamento.InventarioMedicamentoPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RegistrarConsumoMedicamentoUseCaseTest {
  @Test
  void noReservaSiDiagnosticoEsSanitario() {
    UUID id = UUID.randomUUID();
    LocalDate hoy = LocalDate.of(2026, 11, 6);
    DiagnosticoGalpon d =
        new DiagnosticoGalpon(
            id,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Newcastle",
            true,
            null,
            null,
            null,
            null,
            null,
            "Sacrificio",
            hoy,
            null,
            EstadoDiagnostico.ACTIVO,
            null,
            0);
    ConsumoMedicamentoRepositoryPort consumos = mock(ConsumoMedicamentoRepositoryPort.class);
    DiagnosticoQueryPort diagnosticos = mock(DiagnosticoQueryPort.class);
    InventarioMedicamentoPort inventario = mock(InventarioMedicamentoPort.class);
    when(diagnosticos.buscar(id)).thenReturn(Optional.of(d));
    Clock clock = Clock.fixed(Instant.parse("2026-11-06T12:00:00Z"), ZoneOffset.UTC);
    assertThrows(
        TratamientoNoActivoException.class,
        () ->
            new RegistrarConsumoMedicamentoUseCase(consumos, diagnosticos, inventario, clock)
                .registrar(
                    new RegistrarConsumoMedicamentoCommand(
                        id,
                        BigDecimal.ONE,
                        UnidadConsumoMedicamento.UNIDAD,
                        hoy,
                        "vet",
                        UUID.randomUUID())));
    verify(inventario, never()).reservarFefo(any(), any(), any(), any());
    verify(consumos, never()).guardar(any());
  }
}
