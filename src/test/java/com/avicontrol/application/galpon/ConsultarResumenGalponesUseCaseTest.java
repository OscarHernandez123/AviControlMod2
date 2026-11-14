package com.avicontrol.application.galpon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.avicontrol.domain.port.out.modulo1.GalponQueryPort;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsultarResumenGalponesUseCaseTest {
  @Test
  void resumeSinEscriturasNiConsultasDeLote() {
    var galpones = mock(GalponQueryPort.class);
    when(galpones.todos())
        .thenReturn(
            List.of(
                new GalponQueryPort.GalponDatos(UUID.randomUUID(), "A", 100, "PRODUCTIVO"),
                new GalponQueryPort.GalponDatos(UUID.randomUUID(), "B", 100, "DESCONOCIDO")));
    var resultado = new ConsultarResumenGalponesUseCase(galpones).consultar();
    assertEquals(2, resultado.totalRegistrado());
    assertEquals(1, resultado.registrosInconsistentes());
    assertEquals(1, resultado.porEstado().get("PRODUCTIVO"));
    verify(galpones).todos();
    verifyNoMoreInteractions(galpones);
  }
}
