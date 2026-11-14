package com.avicontrol.domain.port.out.alimentacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface PlanNutricionalQueryPort {
  Optional<EtapaDia> buscar(UUID loteId, LocalDate fecha, int edadDias);

  record EtapaDia(
      UUID planId,
      String etapaCrianza,
      String tipoAlimento,
      UUID alimentoId,
      String nombreAlimento,
      BigDecimal racionKgAveDia,
      BigDecimal pesoNominalBultoKg,
      boolean cambioHoy) {}
}
