package com.avicontrol.application.sanitario.enfermedad;

import com.avicontrol.application.support.CurrentActorProvider;
import com.avicontrol.domain.exception.sanitario.EnfermedadNoEncontradaException;
import com.avicontrol.domain.model.sanitario.enfermedad.Enfermedad;
import com.avicontrol.domain.port.out.sanitario.EnfermedadRepositoryPort;
import java.util.UUID;

public final class EditarEnfermedadUseCase {

  private final EnfermedadRepositoryPort repository;
  private final CurrentActorProvider currentActorProvider;

  public EditarEnfermedadUseCase(
      EnfermedadRepositoryPort repository, CurrentActorProvider currentActorProvider) {
    this.repository = repository;
    this.currentActorProvider = currentActorProvider;
  }

  public EnfermedadResult execute(UUID enfermedadId, EnfermedadCommand command) {
    EnfermedadAuthorization.requireVeterinario(currentActorProvider);
    Enfermedad enfermedad =
        repository.buscarPorId(enfermedadId).orElseThrow(EnfermedadNoEncontradaException::new);
    enfermedad.actualizar(
        command.nombre(),
        command.nivelRiesgo(),
        command.tipo(),
        command.descripcionClinica(),
        command.requiereSacrificioSanitario());
    return RegistrarEnfermedadUseCase.toResult(repository.guardar(enfermedad));
  }
}
