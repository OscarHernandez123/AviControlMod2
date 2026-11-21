package com.avicontrol.infrastructure.adapter.in.rest.mortalidad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.OffsetDateTime;

public record RegistrarMortalidadAnormalRequest(
    @NotNull OffsetDateTime fechaHoraEvento,
    @Positive int cantidad,
    @NotBlank String causaProbable,
    String observaciones) {}
