package com.avicontrol.application.sanitario.enfermedad;

import static org.assertj.core.api.Assertions.assertThat;

import com.avicontrol.application.support.CurrentActor;
import com.avicontrol.domain.model.sanitario.enfermedad.Enfermedad;
import com.avicontrol.domain.model.sanitario.enfermedad.NivelRiesgo;
import com.avicontrol.domain.model.sanitario.enfermedad.TipoEnfermedad;
import java.util.HashMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EditarEnfermedadUseCaseTest {
  @Test
  void updatesTheCatalogEntryForFutureConsumers() {
    UUID id = UUID.randomUUID();
    var values = new HashMap<UUID, Enfermedad>();
    values.put(
        id,
        new Enfermedad(id, "Newcastle", NivelRiesgo.ALTO, TipoEnfermedad.VIRAL, "Inicial", true));
    var useCase =
        new EditarEnfermedadUseCase(
            RegistrarEnfermedadUseCaseTest.repo(values),
            () -> new CurrentActor("vet", java.util.Set.of("ROLE_VETERINARIO")));

    var result =
        useCase.execute(
            id,
            new EnfermedadCommand(
                "Newcastle actualizada",
                NivelRiesgo.MEDIO,
                TipoEnfermedad.VIRAL,
                "Actualizada",
                false));

    assertThat(result.nombre()).isEqualTo("Newcastle actualizada");
    assertThat(result.requiereSacrificioSanitario()).isFalse();
  }
}
