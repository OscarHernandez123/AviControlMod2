package com.avicontrol.infrastructure.adapter.in.rest.mortalidad.dto;

import java.util.UUID;

public record RegistroMortalidadResponse(
    UUID id,
    UUID galponId,
    UUID loteId,
    int cantidad,
    String clasificacion,
    int poblacionResultante) {}
