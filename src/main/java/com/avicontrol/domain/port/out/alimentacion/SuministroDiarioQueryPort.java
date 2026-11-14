package com.avicontrol.domain.port.out.alimentacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface SuministroDiarioQueryPort {
  BigDecimal acumulado(UUID loteId, UUID alimentoId, LocalDate fecha);
}
