package com.avicontrol.infrastructure.adapter.in.rest.sacrificio.sanitario.dto;

import java.util.UUID;

public record OrdenSacrificioSanitarioResponse(
    UUID id,
    UUID diagnosticoId,
    UUID galponId,
    UUID loteId,
    String estado,
    Integer poblacionSacrificada) {}
