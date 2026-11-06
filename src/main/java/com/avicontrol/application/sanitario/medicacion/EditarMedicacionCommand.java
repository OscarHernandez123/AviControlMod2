package com.avicontrol.application.sanitario.medicacion;

import java.util.UUID;

public record EditarMedicacionCommand(
    UUID id,
    UUID enfermedadId,
    UUID medicamentoId,
    String dosis,
    int duracionDias,
    String descripcion) {}
