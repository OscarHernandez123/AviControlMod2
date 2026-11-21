package com.avicontrol.infrastructure.adapter.in.rest.sanitario.diagnostico.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RegistrarDiagnosticoRequest(
    @NotNull UUID enfermedadId, UUID medicacionId, @NotBlank String observaciones) {}
