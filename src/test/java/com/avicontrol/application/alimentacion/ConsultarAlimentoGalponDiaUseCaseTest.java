package com.avicontrol.application.alimentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.avicontrol.domain.port.out.alimentacion.InventarioAlimentoPort;
import com.avicontrol.domain.port.out.alimentacion.ObservabilidadAlimentacionPort;
import com.avicontrol.domain.port.out.alimentacion.PlanNutricionalQueryPort;
import com.avicontrol.domain.port.out.alimentacion.SuministroDiarioQueryPort;
import com.avicontrol.domain.port.out.sanitario.medicacion.GalponLoteQueryPort;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ConsultarAlimentoGalponDiaUseCaseTest {
  @Test
  void sinEtapaDevuelveCuotaCeroSinConsultarInventario() {
    UUID galpon = UUID.randomUUID();
    UUID lote = UUID.randomUUID();
    LocalDate hoy = LocalDate.of(2026, 11, 6);
    var galpones = Mockito.mock(GalponLoteQueryPort.class);
    var planes = Mockito.mock(PlanNutricionalQueryPort.class);
    var suministros = Mockito.mock(SuministroDiarioQueryPort.class);
    var inventario = Mockito.mock(InventarioAlimentoPort.class);
    var metrics = Mockito.mock(ObservabilidadAlimentacionPort.class);
    when(galpones.consultar(galpon))
        .thenReturn(
            Optional.of(
                new GalponLoteQueryPort.GalponLote(
                    galpon, "Galpón 1", 10000, "PRODUCTIVA", lote, "L-1", 9000, hoy.minusDays(5))));
    when(planes.buscar(lote, hoy, 6)).thenReturn(Optional.empty());
    var useCase =
        new ConsultarAlimentoGalponDiaUseCase(
            galpones,
            planes,
            suministros,
            inventario,
            metrics,
            Clock.fixed(hoy.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    var resultado = useCase.consultar(galpon, null);
    assertEquals("SIN_ETAPA_NUTRICIONAL", resultado.estado());
    assertEquals(0, resultado.requerimiento().requerimientoKg().signum());
    verifyNoInteractions(inventario, suministros, metrics);
  }
}
