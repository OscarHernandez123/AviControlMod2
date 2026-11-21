package com.avicontrol.infrastructure.adapter.in.rest.sacrificio.productivo.dto;

import java.util.List;
import java.util.UUID;

public record ValidacionAptitudSacrificioResponse(
    UUID galponId,
    UUID loteId,
    boolean apto,
    List<String> motivosNoAptitud,
    String estadoGalponResultante) {}
