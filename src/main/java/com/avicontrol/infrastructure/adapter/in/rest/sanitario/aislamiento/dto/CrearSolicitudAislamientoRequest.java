package com.avicontrol.infrastructure.adapter.in.rest.sanitario.aislamiento.dto;

import jakarta.validation.constraints.NotBlank;

public record CrearSolicitudAislamientoRequest(
    @NotBlank String motivo, @NotBlank String signosClinicos) {}
