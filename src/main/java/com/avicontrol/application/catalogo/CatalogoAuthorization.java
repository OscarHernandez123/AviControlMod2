package com.avicontrol.application.catalogo;

import com.avicontrol.application.support.CurrentActorProvider;
import com.avicontrol.domain.exception.BusinessRuleViolationException;

final class CatalogoAuthorization {
  private CatalogoAuthorization() {}

  static void requireAdministrador(CurrentActorProvider actor) {
    if (!actor.currentActor().authorities().contains("ROLE_ADMINISTRADOR"))
      throw new BusinessRuleViolationException(
          "ACCESO_NO_AUTORIZADO", "El actor no tiene permisos para gestionar el catálogo");
  }
}
