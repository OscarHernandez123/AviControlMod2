package com.avicontrol.domain.model.recepcion;

import java.time.Instant;
import java.util.UUID;

public record RegistroAuditoriaRecepcion(
    UUID id,
    UUID recepcionId,
    String actorId,
    Instant fechaHora,
    String valoresAnteriores,
    String valoresNuevos) {}
