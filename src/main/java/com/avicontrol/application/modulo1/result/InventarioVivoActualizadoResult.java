package com.avicontrol.application.modulo1.result;

import java.util.UUID;

public record InventarioVivoActualizadoResult(
    UUID loteId, int poblacionAnterior, int variacion, int poblacionResultante, String causa) {}
