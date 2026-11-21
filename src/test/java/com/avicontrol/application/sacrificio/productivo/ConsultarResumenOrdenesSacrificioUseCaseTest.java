package com.avicontrol.application.sacrificio.productivo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.avicontrol.domain.port.out.sacrificio.productivo.OrdenSacrificioProductivoRepositoryPort;
import org.junit.jupiter.api.Test;

class ConsultarResumenOrdenesSacrificioUseCaseTest {
  @Test
  void soloLeeUnaVistaConsistente() {
    var ordenes = mock(OrdenSacrificioProductivoRepositoryPort.class);
    when(ordenes.resumen()).thenReturn(new OrdenSacrificioProductivoRepositoryPort.Resumen(6, 2));
    var resultado = new ConsultarResumenOrdenesSacrificioUseCase(ordenes).consultar();
    assertEquals(6, resultado.totalOrdenes());
    assertEquals(2, resultado.ordenesPendientes());
    verify(ordenes).resumen();
    verifyNoMoreInteractions(ordenes);
  }
}
