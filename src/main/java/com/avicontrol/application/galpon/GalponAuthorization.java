package com.avicontrol.application.galpon;

import com.avicontrol.application.support.CurrentActor;
import com.avicontrol.domain.exception.BusinessRuleViolationException;

final class GalponAuthorization {

  private GalponAuthorization() {}

  static void requireAdministrator(CurrentActor actor) {
    requireAny(actor, "ROLE_ADMINISTRADOR");
  }

  static void requireReader(CurrentActor actor) {
    requireAny(actor, "ROLE_ADMINISTRADOR", "ROLE_TRABAJADOR", "ROLE_USUARIO");
  }

  private static void requireAny(CurrentActor actor, String... acceptedAuthorities) {
    for (String authority : acceptedAuthorities) {
      if (actor.authorities().contains(authority)) {
        return;
      }
    }
    throw new BusinessRuleViolationException(
        "ACCESO_NO_AUTORIZADO", "El actor no tiene permisos para consultar galpones");
  }
}
