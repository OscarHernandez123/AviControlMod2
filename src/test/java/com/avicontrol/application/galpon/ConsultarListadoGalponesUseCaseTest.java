package com.avicontrol.application.galpon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.avicontrol.domain.port.out.modulo1.GalponQueryPort;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConsultarListadoGalponesUseCaseTest {
  @Test
  void validaAntesDeConsultarYDevuelvePaginaVacia() {
    var galpones = mock(GalponQueryPort.class);
    var caso = new ConsultarListadoGalponesUseCase(galpones);
    assertThrows(IllegalArgumentException.class, () -> caso.consultar(-1, 20, "nombre,asc"));
    assertThrows(IllegalArgumentException.class, () -> caso.consultar(0, 101, "nombre,asc"));
    assertThrows(IllegalArgumentException.class, () -> caso.consultar(0, 20, "nombre;DROP,asc"));
    verifyNoInteractions(galpones);
    when(galpones.listar(0, 20, "nombre", true))
        .thenReturn(new GalponQueryPort.PaginaGalpones(List.of(), 0));
    assertEquals("No existen galpones disponibles", caso.consultar(0, 20, "nombre,asc").mensaje());
    verify(galpones).listar(0, 20, "nombre", true);
  }
}
