package com.avicontrol.infrastructure.adapter.out.persistence.sanitario.medicacion;

import com.avicontrol.domain.port.out.inventario.InventarioMovimientoQueryPort;
import com.avicontrol.domain.port.out.sanitario.medicacion.MedicamentoCatalogoQueryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MedicamentoDisponibleAdapter implements MedicamentoCatalogoQueryPort {
  private final InventarioMovimientoQueryPort inventario;
  private final Clock clock;

  public MedicamentoDisponibleAdapter(InventarioMovimientoQueryPort inventario, Clock clock) {
    this.inventario = inventario;
    this.clock = clock;
  }

  @Override
  public boolean disponible(UUID id) {
    return inventario.consultar(LocalDate.now(clock)).medicamentos().stream()
        .anyMatch(
            m -> m.medicamentoId().equals(id) && m.stockActual().compareTo(BigDecimal.ZERO) > 0);
  }
}
