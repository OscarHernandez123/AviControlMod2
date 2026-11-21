package com.avicontrol.application.sanitario.enfermedad.result;

import com.avicontrol.domain.model.sanitario.enfermedad.Enfermedad;
import com.avicontrol.domain.model.sanitario.enfermedad.NivelRiesgo;
import com.avicontrol.domain.model.sanitario.enfermedad.TipoEnfermedad;
import java.util.UUID;

public record EnfermedadResult(
    UUID id,
    String nombre,
    NivelRiesgo nivelRiesgo,
    TipoEnfermedad tipo,
    String descripcionClinica,
    boolean requiereSacrificioSanitario) {
  public static EnfermedadResult de(Enfermedad enfermedad) {
    return new EnfermedadResult(
        enfermedad.id(),
        enfermedad.nombre(),
        enfermedad.nivelRiesgo(),
        enfermedad.tipo(),
        enfermedad.descripcionClinica(),
        enfermedad.requiereSacrificioSanitario());
  }
}
