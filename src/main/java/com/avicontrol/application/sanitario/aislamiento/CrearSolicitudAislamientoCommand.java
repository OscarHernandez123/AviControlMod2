package com.avicontrol.application.sanitario.aislamiento;

import java.util.UUID;

public record CrearSolicitudAislamientoCommand(
    UUID galponId, String motivo, String signosClinicos, String trabajador) {}
