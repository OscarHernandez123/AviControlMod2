package com.avicontrol.infrastructure.adapter.in.rest.sacrificio.productivo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CorregirResultadoSacrificioRequest(
    @Min(1) int cantidadFinalPollos,
    @NotNull @DecimalMin("0.01") @Digits(integer = 16, fraction = 2) BigDecimal pesoTotalKg) {}
