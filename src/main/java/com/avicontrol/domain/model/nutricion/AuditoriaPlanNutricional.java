package com.avicontrol.domain.model.nutricion;

import java.time.Instant;
import java.util.UUID;

public record AuditoriaPlanNutricional(
    UUID etapaId,
    String accion,
    String actor,
    String justificacion,
    String valorAnterior,
    String valorNuevo,
    Instant fechaRegistro) {}
