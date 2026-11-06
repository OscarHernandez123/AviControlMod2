package com.avicontrol.application.sanitario.medicacion;

import java.util.UUID;

public record RegistrarMedicacionCommand(
    UUID enfermedadId, UUID medicamentoId, String dosis, int duracionDias, String descripcion) {}
