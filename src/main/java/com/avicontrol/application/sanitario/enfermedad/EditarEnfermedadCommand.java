package com.avicontrol.application.sanitario.enfermedad;

import com.avicontrol.domain.model.sanitario.enfermedad.NivelRiesgo;
import com.avicontrol.domain.model.sanitario.enfermedad.TipoEnfermedad;
import java.util.UUID;

public record EditarEnfermedadCommand(
    UUID id,
    String nombre,
    NivelRiesgo nivelRiesgo,
    TipoEnfermedad tipo,
    String descripcionClinica,
    Boolean requiereSacrificioSanitario) {}
