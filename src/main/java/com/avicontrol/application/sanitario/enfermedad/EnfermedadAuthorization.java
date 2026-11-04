package com.avicontrol.application.sanitario.enfermedad;

import com.avicontrol.application.support.CurrentActorProvider;
import com.avicontrol.domain.exception.BusinessRuleViolationException;

final class EnfermedadAuthorization {

  private EnfermedadAuthorization() {}

  static void requireVeterinario(CurrentActorProvider currentActorProvider) {
    if (!currentActorProvider.currentActor().authorities().contains("ROLE_VETERINARIO")) {
      throw new BusinessRuleViolationException(
          "ACCESO_NO_AUTORIZADO", "El actor no tiene permisos para gestionar enfermedades");
    }
  }
}
