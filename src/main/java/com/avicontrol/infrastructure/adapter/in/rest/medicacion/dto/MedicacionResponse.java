package com.avicontrol.infrastructure.adapter.in.rest.medicacion.dto;

import java.util.UUID;

public record MedicacionResponse(
    UUID id,
    UUID enfermedadId,
    UUID medicamentoId,
    String dosis,
    int duracionDias,
    String descripcion) {}
