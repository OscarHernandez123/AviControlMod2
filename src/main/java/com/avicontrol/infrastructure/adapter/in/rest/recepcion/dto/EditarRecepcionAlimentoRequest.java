package com.avicontrol.infrastructure.adapter.in.rest.recepcion.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record EditarRecepcionAlimentoRequest(
    @NotNull UUID alimentoId,
    @NotNull LocalDate fechaVencimiento,
    @Positive int cantidadBultos,
    @NotNull @Positive BigDecimal pesoPorBultoKg,
    @NotNull @Positive BigDecimal precioNetoPorBulto,
    @NotNull @PositiveOrZero BigDecimal impuestoPorcentaje) {}
