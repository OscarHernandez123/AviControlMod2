package com.avicontrol.infrastructure.adapter.in.rest.sanitario;

import com.avicontrol.domain.model.sanitario.enfermedad.NivelRiesgo;
import com.avicontrol.domain.model.sanitario.enfermedad.TipoEnfermedad;
import java.util.UUID;

record EnfermedadResponse(
    UUID id,
    String nombre,
    NivelRiesgo nivelRiesgo,
    TipoEnfermedad tipo,
    String descripcionClinica,
    boolean requiereSacrificioSanitario) {}
