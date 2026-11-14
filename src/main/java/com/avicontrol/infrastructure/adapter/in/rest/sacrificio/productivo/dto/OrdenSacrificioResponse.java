package com.avicontrol.infrastructure.adapter.in.rest.sacrificio.productivo.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OrdenSacrificioResponse(
    UUID id, UUID galponId, UUID loteId, String estado, OffsetDateTime fechaHoraProgramada) {}
