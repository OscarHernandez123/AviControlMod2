package com.avicontrol.application.modulo1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.avicontrol.application.modulo1.result.InventarioVivoActualizadoResult;
import com.avicontrol.domain.exception.modulo1.PoblacionVivaInsuficienteException;
import com.avicontrol.domain.port.out.modulo1.ActualizarInventarioVivoPort;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActualizarInventarioVivoUseCaseTest {
  @Mock ActualizarInventarioVivoPort port;
  @InjectMocks ActualizarInventarioVivoUseCase useCase;

  @Test
  void descuentaPoblacionYRetornaResultado() {
    UUID loteId = UUID.randomUUID();
    UUID galponId = UUID.randomUUID();
    UUID key = UUID.randomUUID();
    var cmd =
        new ActualizarInventarioVivoCommand(
            loteId, galponId, 10, "MORTALIDAD", "trabajador", 1L, key);
    when(port.descontar(
            new ActualizarInventarioVivoPort.Descuento(
                loteId, galponId, 10, "MORTALIDAD", "trabajador", 1L, key)))
        .thenReturn(
            new ActualizarInventarioVivoPort.Resultado(loteId, 1000, -10, 990, "MORTALIDAD"));

    InventarioVivoActualizadoResult result = useCase.actualizar(cmd);

    assertThat(result.poblacionAnterior()).isEqualTo(1000);
    assertThat(result.variacion()).isEqualTo(-10);
    assertThat(result.poblacionResultante()).isEqualTo(990);
  }

  @Test
  void rechazaComandoNuloOPropagaPoblacionInsuficiente() {
    assertThatThrownBy(() -> useCase.actualizar(null))
        .isInstanceOf(IllegalArgumentException.class);

    UUID loteId = UUID.randomUUID();
    UUID galponId = UUID.randomUUID();
    UUID key = UUID.randomUUID();
    var cmd =
        new ActualizarInventarioVivoCommand(
            loteId, galponId, 5000, "MORTALIDAD", "trabajador", 1L, key);
    when(port.descontar(org.mockito.ArgumentMatchers.any()))
        .thenThrow(new PoblacionVivaInsuficienteException());

    assertThatThrownBy(() -> useCase.actualizar(cmd))
        .isInstanceOf(PoblacionVivaInsuficienteException.class);
  }
}
