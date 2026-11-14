package com.avicontrol.infrastructure.adapter.in.rest.mortalidad.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RegistroMortalidadAnormalResponse(
    UUID id, UUID galponId, BigDecimal porcentajeMortalidad, String severidad, UUID alertaId) {}
