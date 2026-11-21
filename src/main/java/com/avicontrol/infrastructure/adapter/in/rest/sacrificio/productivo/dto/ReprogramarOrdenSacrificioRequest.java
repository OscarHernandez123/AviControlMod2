package com.avicontrol.infrastructure.adapter.in.rest.sacrificio.productivo.dto;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record ReprogramarOrdenSacrificioRequest(@NotNull OffsetDateTime fechaHoraProgramada) {}
