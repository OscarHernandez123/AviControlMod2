package com.avicontrol.application.sanitario.enfermedad;

import com.avicontrol.application.support.CurrentActorProvider;
import com.avicontrol.domain.model.sanitario.enfermedad.Enfermedad;
import com.avicontrol.domain.port.out.sanitario.EnfermedadRepositoryPort;
import java.util.UUID;

public final class RegistrarEnfermedadUseCase {

  private final EnfermedadRepositoryPort repository;
  private final CurrentActorProvider currentActorProvider;

  public RegistrarEnfermedadUseCase(
      EnfermedadRepositoryPort repository, CurrentActorProvider currentActorProvider) {
    this.repository = repository;
    this.currentActorProvider = currentActorProvider;
  }

  public EnfermedadResult execute(EnfermedadCommand command) {
    EnfermedadAuthorization.requireVeterinario(currentActorProvider);
    Enfermedad enfermedad =
        repository.guardar(
            new Enfermedad(
                UUID.randomUUID(),
                command.nombre(),
                command.nivelRiesgo(),
                command.tipo(),
                command.descripcionClinica(),
                command.requiereSacrificioSanitario()));
    return toResult(enfermedad);
  }

  static EnfermedadResult toResult(Enfermedad enfermedad) {
    return new EnfermedadResult(
        enfermedad.id(),
        enfermedad.nombre(),
        enfermedad.nivelRiesgo(),
        enfermedad.tipo(),
        enfermedad.descripcionClinica(),
        enfermedad.requiereSacrificioSanitario());
  }
}
