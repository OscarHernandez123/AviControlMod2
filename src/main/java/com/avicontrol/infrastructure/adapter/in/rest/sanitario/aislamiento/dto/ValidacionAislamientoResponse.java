package com.avicontrol.infrastructure.adapter.in.rest.sanitario.aislamiento.dto;

import com.avicontrol.domain.model.sanitario.aislamiento.EstadoSolicitudAislamiento;
import java.util.UUID;

public record ValidacionAislamientoResponse(
    UUID id, EstadoSolicitudAislamiento estado, UUID galponId, String estadoGalponResultante) {}
