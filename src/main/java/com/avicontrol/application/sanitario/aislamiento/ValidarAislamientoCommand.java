package com.avicontrol.application.sanitario.aislamiento;

import com.avicontrol.domain.model.sanitario.aislamiento.DecisionAislamiento;
import java.util.UUID;

public record ValidarAislamientoCommand(
    UUID solicitudId, DecisionAislamiento decision, String observaciones, String veterinario) {}
