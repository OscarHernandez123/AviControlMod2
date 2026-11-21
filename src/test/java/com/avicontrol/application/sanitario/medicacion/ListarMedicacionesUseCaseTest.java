package com.avicontrol.application.sanitario.medicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.avicontrol.domain.model.sanitario.medicacion.Medicacion;
import com.avicontrol.domain.port.out.sanitario.medicacion.MedicacionRepositoryPort;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListarMedicacionesUseCaseTest {
  @Mock MedicacionRepositoryPort repository;
  @InjectMocks ListarMedicacionesUseCase useCase;

  @Test
  void listaMedicacionesMapeadasAResult() {
    Instant ahora = Instant.now();
    Medicacion medicacion =
        new Medicacion(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "10 ml / 100 L",
            7,
            "Vía oral",
            0L,
            ahora,
            ahora,
            "Enrofloxacina",
            "Enrofloxacina",
            null);
    when(repository.listar()).thenReturn(List.of(medicacion));

    var resultado = useCase.listar();

    assertThat(resultado).hasSize(1);
    assertThat(resultado.get(0).nombreMedicamento()).isEqualTo("Enrofloxacina");
  }
}
