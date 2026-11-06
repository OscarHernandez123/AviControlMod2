package com.avicontrol.application.inventario.port;

import java.math.BigDecimal;
import java.util.UUID;

public interface AjustarEntradaInventarioPort {
  boolean tieneMovimientosPosteriores(UUID recepcionId);

  void ajustarEntrada(
      UUID recepcionId,
      String tipoProducto,
      UUID productoId,
      BigDecimal cantidad,
      String unidad,
      String actor);
}
