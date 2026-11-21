package com.avicontrol.domain.port.out.inventario;

import java.util.List;

public interface ResumenInventarioQueryPort {
  List<RecepcionReciente> recientes();

  record RecepcionReciente(
      String codigoLote,
      String alimento,
      int cantidadBultos,
      java.math.BigDecimal precioPorBulto,
      java.math.BigDecimal pesoPorBultoKg,
      java.time.LocalDate fechaIngreso) {}
}
