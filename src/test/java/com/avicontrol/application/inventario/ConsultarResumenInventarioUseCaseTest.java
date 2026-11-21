package com.avicontrol.application.inventario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.avicontrol.domain.port.out.inventario.ResumenInventarioQueryPort;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConsultarResumenInventarioUseCaseTest {
  @Test
  void calculaPorcentajeConOcupacionConfigurada() {
    ResumenInventarioQueryPort recepciones = mock(ResumenInventarioQueryPort.class);
    when(recepciones.recientes()).thenReturn(List.of());

    var resumen =
        new ConsultarResumenInventarioUseCase(
                recepciones, new BigDecimal("10000"), new BigDecimal("2500"))
            .consultar();

    assertEquals(0, new BigDecimal("25.00").compareTo(resumen.porcentajeOcupacion()));
    assertEquals(List.of(), resumen.recepcionesAlimentoRecientes());
  }

  @Test
  void noInventaOcupacionCuandoNoHayFuente() {
    ResumenInventarioQueryPort recepciones = mock(ResumenInventarioQueryPort.class);
    when(recepciones.recientes()).thenReturn(List.of());

    var resumen =
        new ConsultarResumenInventarioUseCase(
                recepciones, new BigDecimal("10000"), new BigDecimal("-1"))
            .consultar();

    assertNull(resumen.ocupacionActualKg());
    assertNull(resumen.porcentajeOcupacion());
  }
}
