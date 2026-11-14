package com.avicontrol.infrastructure.adapter.in.rest.sacrificio.productivo.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ResultadoSacrificioResponse(
    UUID id,
    UUID ordenId,
    UUID galponId,
    UUID loteId,
    int cantidadFinalPollos,
    BigDecimal pesoTotalKg,
    String estado,
    Instant fechaRegistro,
    Instant fechaCorreccion) {}
