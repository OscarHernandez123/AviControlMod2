package com.avicontrol.domain.model.mortalidad;

import java.time.Instant;
import java.util.UUID;

public record RegistroMortalidad(
    UUID id,
    UUID galponId,
    UUID loteId,
    Instant fechaHoraEvento,
    Instant fechaHoraRegistro,
    int cantidad,
    CausaMortalidad causa,
    String observaciones,
    String responsable,
    ClasificacionMortalidad clasificacion,
    int poblacionAnterior,
    int poblacionResultante) {}
