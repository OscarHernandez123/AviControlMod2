package com.avicontrol.application.alimentacion.result;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ResumenAlimentoDiarioResult(
    LocalDate fecha,
    BigDecimal requerimientoTotalKg,
    BigDecimal requerimientoTotalBultos,
    String estadoBodega,
    List<Fila> galpones) {
  public ResumenAlimentoDiarioResult {
    galpones = List.copyOf(galpones);
  }

  public record Fila(AlimentoDiarioGalponResult detalle, String estadoBodega) {}
}
