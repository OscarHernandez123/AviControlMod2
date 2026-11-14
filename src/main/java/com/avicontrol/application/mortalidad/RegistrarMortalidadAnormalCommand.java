package com.avicontrol.application.mortalidad;

import java.time.Instant;
import java.util.UUID;

public record RegistrarMortalidadAnormalCommand(
    UUID galponId,
    Instant fechaHoraEvento,
    int cantidad,
    String causaProbable,
    String observaciones,
    String responsable,
    UUID idempotencyKey) {}
