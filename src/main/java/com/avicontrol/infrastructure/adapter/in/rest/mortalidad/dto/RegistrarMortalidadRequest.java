package com.avicontrol.infrastructure.adapter.in.rest.mortalidad.dto;

import com.avicontrol.domain.model.mortalidad.CausaMortalidad;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.OffsetDateTime;

public record RegistrarMortalidadRequest(
    @NotNull OffsetDateTime fechaHoraEvento,
    @Positive int cantidad,
    @NotNull CausaMortalidad causa,
    String observaciones) {}
