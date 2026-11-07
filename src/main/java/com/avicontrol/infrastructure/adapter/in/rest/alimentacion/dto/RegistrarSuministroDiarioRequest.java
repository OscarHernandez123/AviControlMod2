package com.avicontrol.infrastructure.adapter.in.rest.alimentacion.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RegistrarSuministroDiarioRequest(
    @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal cantidadKg,
    @NotNull LocalDate fechaAplicacion,
    @NotNull UUID idempotencyKey) {}
