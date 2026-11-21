package com.avicontrol.infrastructure.adapter.in.rest.medicamento.dto;

import com.avicontrol.domain.model.medicamento.UnidadConsumoMedicamento;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ConsumoMedicamentoResponse(
    UUID id,
    UUID diagnosticoId,
    UUID galponId,
    UUID loteId,
    UUID medicamentoId,
    BigDecimal cantidadIngresada,
    UnidadConsumoMedicamento unidadIngresada,
    BigDecimal cantidadUnidadBase,
    UnidadConsumoMedicamento unidadBase,
    LocalDate fechaAplicacion,
    Instant fechaRegistro,
    String actor,
    List<DetalleConsumoMedicamentoResponse> detallesInventario) {}
