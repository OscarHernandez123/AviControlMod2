package com.avicontrol.domain.model.alimentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RequerimientoDiarioGalponTest {
  @Test
  void calculaRacionSaldoYBultosFraccionarios() {
    var r =
        RequerimientoDiarioGalpon.calcular(
            10000,
            new BigDecimal("0.1200"),
            new BigDecimal("40"),
            new BigDecimal("400"),
            new BigDecimal("1600"));
    assertEquals(0, r.racionGramosAve().compareTo(new BigDecimal("120")));
    assertEquals(0, r.requerimientoKg().compareTo(new BigDecimal("1200")));
    assertEquals(0, r.requerimientoBultos().compareTo(new BigDecimal("30")));
    assertEquals(0, r.saldoKg().compareTo(new BigDecimal("800")));
    assertEquals(0, r.saldoBultos().compareTo(new BigDecimal("20")));
    assertEquals(
        0,
        RequerimientoDiarioGalpon.bultos(new BigDecimal("430"), new BigDecimal("50"))
            .compareTo(new BigDecimal("8.6")));
  }

  @Test
  void rechazaDatosInvalidosYNoDejaSaldoNegativo() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            RequerimientoDiarioGalpon.calcular(
                5, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    var r =
        RequerimientoDiarioGalpon.calcular(
            1, BigDecimal.ONE, BigDecimal.ONE, new BigDecimal("2"), BigDecimal.ZERO);
    assertEquals(0, r.saldoKg().signum());
  }
}
