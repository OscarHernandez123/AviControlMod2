package com.avicontrol.application.recepcion;

import com.avicontrol.application.support.CurrentActorProvider;
import com.avicontrol.domain.exception.BusinessRuleViolationException;

final class RecepcionAuthorization {
  private RecepcionAuthorization() {}

  static void requireAdministrador(CurrentActorProvider actor) {
    if (!actor.currentActor().authorities().contains("ROLE_ADMINISTRADOR"))
      throw new BusinessRuleViolationException(
          "ACCESO_NO_AUTORIZADO", "El actor no tiene permisos para gestionar recepciones");
  }
}
