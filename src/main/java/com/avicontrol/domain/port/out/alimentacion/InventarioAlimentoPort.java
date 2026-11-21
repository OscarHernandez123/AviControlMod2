package com.avicontrol.domain.port.out.alimentacion;

import com.avicontrol.domain.model.alimentacion.SuministroDiarioAlimento.Detalle;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface InventarioAlimentoPort {
  BigDecimal disponible(UUID alimentoId, String tipoAlimento, LocalDate fecha);

  List<Detalle> reservarFefo(
      UUID alimentoId, String tipoAlimento, BigDecimal cantidadKg, LocalDate fecha);
}
