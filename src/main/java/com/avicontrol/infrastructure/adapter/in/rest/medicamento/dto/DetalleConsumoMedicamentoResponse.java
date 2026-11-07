package com.avicontrol.infrastructure.adapter.in.rest.medicamento.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record DetalleConsumoMedicamentoResponse(
    UUID recepcionId,
    BigDecimal cantidadUnidadBase,
    BigDecimal precioNetoHistoricoUnidadBase,
    String moneda) {}
