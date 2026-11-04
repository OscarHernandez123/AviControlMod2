package com.avicontrol.application.sanitario.enfermedad;

import static org.assertj.core.api.Assertions.assertThat;

import com.avicontrol.application.support.CurrentActor;
import com.avicontrol.domain.model.sanitario.enfermedad.Enfermedad;
import com.avicontrol.domain.model.sanitario.enfermedad.NivelRiesgo;
import com.avicontrol.domain.model.sanitario.enfermedad.TipoEnfermedad;
import com.avicontrol.domain.port.out.sanitario.EnfermedadRepositoryPort;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RegistrarEnfermedadUseCaseTest {
  @Test
  void createsAnEnfermedadWithAnExplicitFalseDecision() {
    Map<UUID, Enfermedad> values = new HashMap<>();
    EnfermedadRepositoryPort repository = repo(values);
    var useCase =
        new RegistrarEnfermedadUseCase(
            repository, () -> new CurrentActor("vet", java.util.Set.of("ROLE_VETERINARIO")));

    var result =
        useCase.execute(
            new EnfermedadCommand(
                "Coccidiosis",
                NivelRiesgo.MEDIO,
                TipoEnfermedad.PARASITARIA,
                "Infección intestinal",
                false));

    assertThat(result.requiereSacrificioSanitario()).isFalse();
    assertThat(values).containsKey(result.id());
  }

  static EnfermedadRepositoryPort repo(Map<UUID, Enfermedad> values) {
    return new EnfermedadRepositoryPort() {
      @Override
      public Enfermedad guardar(Enfermedad enfermedad) {
        values.put(enfermedad.id(), enfermedad);
        return enfermedad;
      }

      @Override
      public Optional<Enfermedad> buscarPorId(UUID id) {
        return Optional.ofNullable(values.get(id));
      }
    };
  }
}
