package com.avicontrol.infrastructure.adapter.in.rest.alimentacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ResumenAlimentoDiarioResponse(
    LocalDate fecha,
    BigDecimal requerimientoTotalKg,
    BigDecimal requerimientoTotalBultos,
    String estadoBodega,
    List<Fila> galpones) {
  public record Fila(
      UUID galponId,
      String nombreGalpon,
      String loteCodigo,
      String tipoAlimento,
      String nombreAlimento,
      BigDecimal cuotaKg,
      BigDecimal cuotaBultos,
      String estadoBodega) {}
}
