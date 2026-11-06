package com.avicontrol.infrastructure.adapter.in.rest.recepcion.dto;

import com.avicontrol.domain.model.catalogo.EtapaCrianza;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RegistrarRecepcionAlimentoRequest(
    @NotNull UUID alimentoId,
    @NotNull EtapaCrianza tipoAlimento,
    @NotBlank String codigoLote,
    @NotNull LocalDate fechaIngreso,
    @NotNull LocalDate fechaVencimiento,
    @Positive int cantidadBultos,
    @NotNull @Positive BigDecimal pesoPorBultoKg,
    @NotNull @Positive BigDecimal precioNetoPorBulto,
    @NotNull @PositiveOrZero BigDecimal impuestoPorcentaje) {}
