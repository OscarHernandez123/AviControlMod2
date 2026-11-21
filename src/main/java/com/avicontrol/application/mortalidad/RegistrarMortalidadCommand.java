package com.avicontrol.application.mortalidad;

import com.avicontrol.domain.model.mortalidad.CausaMortalidad;
import java.time.Instant;
import java.util.UUID;

public record RegistrarMortalidadCommand(
    UUID galponId,
    Instant fechaHoraEvento,
    int cantidad,
    CausaMortalidad causa,
    String observaciones,
    String responsable,
    UUID idempotencyKey) {}
