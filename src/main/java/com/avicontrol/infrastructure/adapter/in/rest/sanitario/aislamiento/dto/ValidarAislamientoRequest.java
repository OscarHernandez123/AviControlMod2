package com.avicontrol.infrastructure.adapter.in.rest.sanitario.aislamiento.dto;

import com.avicontrol.domain.model.sanitario.aislamiento.DecisionAislamiento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ValidarAislamientoRequest(
    @NotNull DecisionAislamiento decision, @NotBlank String observaciones) {}
