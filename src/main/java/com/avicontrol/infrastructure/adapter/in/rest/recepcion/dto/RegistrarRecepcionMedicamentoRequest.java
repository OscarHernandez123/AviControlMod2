package com.avicontrol.infrastructure.adapter.in.rest.recepcion.dto;

import com.avicontrol.domain.model.catalogo.UnidadMedidaMedicamento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RegistrarRecepcionMedicamentoRequest(
    @NotNull UUID medicamentoId,
    @NotBlank String codigoLote,
    @NotBlank String presentacion,
    @NotNull LocalDate fechaIngreso,
    @NotNull LocalDate fechaVencimiento,
    @Positive int cantidadPresentaciones,
    @NotNull @Positive BigDecimal contenidoPorPresentacion,
    @NotNull UnidadMedidaMedicamento unidadContenido,
    @NotNull @Positive BigDecimal precioNetoPorPresentacion,
    @NotNull @PositiveOrZero BigDecimal impuestoPorcentaje) {}
