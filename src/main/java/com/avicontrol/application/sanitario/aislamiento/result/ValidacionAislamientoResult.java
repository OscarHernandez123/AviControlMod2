package com.avicontrol.application.sanitario.aislamiento.result;

import com.avicontrol.domain.model.sanitario.aislamiento.EstadoSolicitudAislamiento;
import java.util.UUID;

public record ValidacionAislamientoResult(
    UUID id, EstadoSolicitudAislamiento estado, UUID galponId, String estadoGalponResultante) {}
