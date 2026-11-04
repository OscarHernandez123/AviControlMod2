package com.avicontrol.application.sanitario.enfermedad;

import com.avicontrol.domain.model.sanitario.enfermedad.NivelRiesgo;
import com.avicontrol.domain.model.sanitario.enfermedad.TipoEnfermedad;

public record EnfermedadCommand(
    String nombre,
    NivelRiesgo nivelRiesgo,
    TipoEnfermedad tipo,
    String descripcionClinica,
    boolean requiereSacrificioSanitario) {}
