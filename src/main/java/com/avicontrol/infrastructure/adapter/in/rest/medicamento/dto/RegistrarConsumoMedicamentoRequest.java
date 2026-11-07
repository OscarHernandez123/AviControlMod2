package com.avicontrol.infrastructure.adapter.in.rest.medicamento.dto;

import com.avicontrol.domain.model.medicamento.UnidadConsumoMedicamento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RegistrarConsumoMedicamentoRequest(
    @NotNull @Positive BigDecimal cantidad,
    @NotNull UnidadConsumoMedicamento unidad,
    @NotNull LocalDate fechaAplicacion) {}
