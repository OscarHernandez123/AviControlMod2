package com.avicontrol.application.sanitario.diagnostico;

import java.util.UUID;

public record RegistrarDiagnosticoCommand(
    UUID galponId, UUID enfermedadId, UUID medicacionId, String observaciones) {}
