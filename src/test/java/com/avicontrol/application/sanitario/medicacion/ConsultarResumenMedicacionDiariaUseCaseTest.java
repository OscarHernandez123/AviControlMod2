package com.avicontrol.application.sanitario.medicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.avicontrol.application.sanitario.medicacion.result.MedicacionDiariaGalponResult;
import com.avicontrol.domain.model.sanitario.medicacion.MedicacionDiaria;
import com.avicontrol.domain.model.sanitario.medicacion.ProgresoTratamiento;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ConsultarResumenMedicacionDiariaUseCaseTest {
  @Test
  void cuentaGalponesSeparadosDeTratamientosYEliminaSeleccionDuplicada() {
    LocalDate fecha = LocalDate.of(2026, 11, 6);
    Clock clock = Clock.fixed(fecha.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    var detalle = Mockito.mock(ConsultarMedicacionDiariaGalponUseCase.class);
    UUID id = UUID.randomUUID();
    var tratamiento =
        new MedicacionDiaria(
            UUID.randomUUID(),
            "L-1",
            new ProgresoTratamiento(2, 5),
            "Enfermedad",
            "Medicamento",
            "Principio",
            "1 ml",
            "Agua",
            true);
    when(detalle.consultar(id, fecha))
        .thenReturn(
            new MedicacionDiariaGalponResult(
                id,
                "Galpón 1",
                100,
                "PRODUCTIVA",
                UUID.randomUUID(),
                "L-1",
                80,
                4,
                fecha,
                "CON_MEDICACION_ACTIVA",
                null,
                List.of(tratamiento, tratamiento)));
    var result =
        new ConsultarResumenMedicacionDiariaUseCase(detalle, clock)
            .consultar(List.of(id, id), null);
    assertEquals(1, result.galponesConTratamiento());
    assertEquals(2, result.cantidadTratamientos());
    assertEquals(1, result.galpones().size());
    assertEquals("Medicamento en Galpón 1", result.resumen());
  }
}
