package com.avicontrol.application.sacrificio.productivo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.avicontrol.domain.model.sacrificio.productivo.EstadoResultadoSacrificio;
import com.avicontrol.domain.model.sacrificio.productivo.ResultadoSacrificioProductivo;
import com.avicontrol.domain.port.out.sacrificio.productivo.ResultadoSacrificioRepositoryPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsultarResultadoSacrificioUseCaseTest {
  @Mock ResultadoSacrificioRepositoryPort resultados;
  @InjectMocks ConsultarResultadoSacrificioUseCase useCase;

  @Test
  void consultaResultadoPorOrdenOLanza404SiNoExiste() {
    UUID ordenId = UUID.randomUUID();
    ResultadoSacrificioProductivo res =
        new ResultadoSacrificioProductivo(
            UUID.randomUUID(),
            ordenId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            900,
            new BigDecimal("2100.50"),
            EstadoResultadoSacrificio.DISPONIBLE,
            Instant.now(),
            null,
            0L);
    when(resultados.buscarPorOrden(ordenId)).thenReturn(Optional.of(res));

    var dto = useCase.consultar(ordenId);
    assertThat(dto.cantidadFinalPollos()).isEqualTo(900);
    assertThat(dto.estado()).isEqualTo("DISPONIBLE");

    UUID otroId = UUID.randomUUID();
    when(resultados.buscarPorOrden(otroId)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> useCase.consultar(otroId)).isInstanceOf(NoSuchElementException.class);
  }
}
