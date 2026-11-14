package com.avicontrol.domain.model.sanitario.aislamiento;

import java.time.LocalDate;
import java.util.UUID;

public record GalponLoteAislamiento(
    UUID galponId, UUID loteId, String estado, int poblacionViva, LocalDate fechaIngreso) {}
