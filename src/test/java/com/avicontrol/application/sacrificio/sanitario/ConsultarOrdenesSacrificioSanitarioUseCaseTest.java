package com.avicontrol.application.sacrificio.sanitario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.avicontrol.domain.model.sacrificio.sanitario.EstadoOrdenSacrificioSanitario;
import com.avicontrol.domain.model.sacrificio.sanitario.OrdenSacrificioSanitario;
import com.avicontrol.domain.port.out.sacrificio.sanitario.OrdenSacrificioSanitarioRepositoryPort;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsultarOrdenesSacrificioSanitarioUseCaseTest {
  @Mock OrdenSacrificioSanitarioRepositoryPort ordenes;
  @InjectMocks ConsultarOrdenesSacrificioSanitarioUseCase useCase;

  @Test
  void listaOrdenesSanitariasMapeadasAResult() {
    OrdenSacrificioSanitario orden =
        new OrdenSacrificioSanitario(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            EstadoOrdenSacrificioSanitario.PENDIENTE,
            null,
            Instant.now(),
            null,
            0L);
    when(ordenes.listar()).thenReturn(List.of(orden));

    var resultado = useCase.listar();

    assertThat(resultado).hasSize(1);
    assertThat(resultado.get(0).estado()).isEqualTo("PENDIENTE");
  }
}
