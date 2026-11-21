package com.avicontrol.application.sanitario.enfermedad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.avicontrol.domain.model.sanitario.enfermedad.Enfermedad;
import com.avicontrol.domain.model.sanitario.enfermedad.NivelRiesgo;
import com.avicontrol.domain.model.sanitario.enfermedad.TipoEnfermedad;
import com.avicontrol.domain.port.out.sanitario.enfermedad.EnfermedadRepositoryPort;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListarEnfermedadesUseCaseTest {
  @Mock EnfermedadRepositoryPort repository;
  @InjectMocks ListarEnfermedadesUseCase useCase;

  @Test
  void listaEnfermedadesMapeadasAResult() {
    Instant ahora = Instant.now();
    Enfermedad enfermedad =
        new Enfermedad(
            UUID.randomUUID(),
            "Gumboro",
            NivelRiesgo.ALTO,
            TipoEnfermedad.VIRAL,
            "Descripción clínica",
            false,
            0L,
            ahora,
            ahora);
    when(repository.listar()).thenReturn(List.of(enfermedad));

    var resultado = useCase.listar();

    assertThat(resultado).hasSize(1);
    assertThat(resultado.get(0).nombre()).isEqualTo("Gumboro");
  }
}
