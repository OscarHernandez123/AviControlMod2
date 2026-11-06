package com.avicontrol.infrastructure.adapter.in.rest.medicacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record RegistrarMedicacionRequest(
    @NotNull UUID enfermedadId,
    @NotNull UUID medicamentoId,
    @NotBlank String dosis,
    @NotNull @Positive Integer duracionDias,
    @NotBlank String descripcion) {}
