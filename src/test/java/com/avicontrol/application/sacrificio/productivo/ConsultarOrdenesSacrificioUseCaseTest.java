package com.avicontrol.application.sacrificio.productivo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.avicontrol.domain.model.sacrificio.productivo.OrdenSacrificioProductivo;
import com.avicontrol.domain.port.out.sacrificio.productivo.OrdenSacrificioProductivoRepositoryPort;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsultarOrdenesSacrificioUseCaseTest {
  @Mock OrdenSacrificioProductivoRepositoryPort ordenes;
  @InjectMocks ConsultarOrdenesSacrificioUseCase useCase;

  @Test
  void listaOrdenesMapeadasAResult() {
    Instant ahora = Instant.parse("2026-11-20T15:00:00Z");
    OrdenSacrificioProductivo orden =
        OrdenSacrificioProductivo.programar(
            UUID.randomUUID(), UUID.randomUUID(), ahora.plusSeconds(3600), ahora);
    when(ordenes.listar()).thenReturn(List.of(orden));

    var resultado = useCase.listar();

    assertThat(resultado).hasSize(1);
    assertThat(resultado.get(0).estado()).isEqualTo("PROGRAMADA");
  }
}
