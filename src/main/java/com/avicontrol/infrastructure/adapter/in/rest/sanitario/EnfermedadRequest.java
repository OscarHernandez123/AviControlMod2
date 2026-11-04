package com.avicontrol.infrastructure.adapter.in.rest.sanitario;

import com.avicontrol.domain.model.sanitario.enfermedad.NivelRiesgo;
import com.avicontrol.domain.model.sanitario.enfermedad.TipoEnfermedad;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

record EnfermedadRequest(
    @NotBlank String nombre,
    @NotNull NivelRiesgo nivelRiesgo,
    @NotNull TipoEnfermedad tipo,
    @NotBlank String descripcionClinica,
    @NotNull Boolean requiereSacrificioSanitario) {}
