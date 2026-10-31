package com.avicontrol.application.galpon.result;

import com.avicontrol.domain.model.lote.Edad;
import java.time.LocalDate;
import java.util.UUID;

public record LoteActualResult(UUID id, int poblacionActual, LocalDate fechaIngreso, Edad edad) {}
