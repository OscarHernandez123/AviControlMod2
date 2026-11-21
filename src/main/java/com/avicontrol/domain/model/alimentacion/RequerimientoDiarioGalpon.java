package com.avicontrol.domain.model.alimentacion;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record RequerimientoDiarioGalpon(
    BigDecimal racionGramosAve,
    BigDecimal requerimientoKg,
    BigDecimal requerimientoBultos,
    BigDecimal suministradoKg,
    BigDecimal saldoKg,
    BigDecimal saldoBultos,
    BigDecimal disponibilidadKg,
    BigDecimal disponibilidadBultos) {

  public static RequerimientoDiarioGalpon calcular(
      int poblacion,
      BigDecimal racionKgAve,
      BigDecimal pesoBultoKg,
      BigDecimal suministradoKg,
      BigDecimal disponibilidadKg) {
    if (poblacion < 0
        || racionKgAve == null
        || pesoBultoKg == null
        || racionKgAve.signum() <= 0
        || pesoBultoKg.signum() <= 0
        || suministradoKg == null
        || suministradoKg.signum() < 0
        || disponibilidadKg == null
        || disponibilidadKg.signum() < 0)
      throw new IllegalArgumentException("Datos de requerimiento inválidos");
    BigDecimal requerido = racionKgAve.multiply(BigDecimal.valueOf(poblacion));
    BigDecimal saldo = requerido.subtract(suministradoKg).max(BigDecimal.ZERO);
    return new RequerimientoDiarioGalpon(
        racionKgAve.multiply(BigDecimal.valueOf(1000)),
        requerido,
        bultos(requerido, pesoBultoKg),
        suministradoKg,
        saldo,
        bultos(saldo, pesoBultoKg),
        disponibilidadKg,
        bultos(disponibilidadKg, pesoBultoKg));
  }

  public static BigDecimal bultos(BigDecimal kg, BigDecimal pesoBultoKg) {
    return kg.divide(pesoBultoKg, 4, RoundingMode.HALF_UP);
  }
}
