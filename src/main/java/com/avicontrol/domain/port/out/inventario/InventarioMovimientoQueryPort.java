package com.avicontrol.domain.port.out.inventario;

import com.avicontrol.domain.model.inventario.InventarioActual;
import java.time.LocalDate;

public interface InventarioMovimientoQueryPort {
  InventarioActual consultar(LocalDate fecha);
}
