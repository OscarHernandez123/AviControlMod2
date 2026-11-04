package com.avicontrol.domain.model.inventario;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MovimientoInventarioTest {
  @Test
  void calculatesAConfirmedEntryAsPositiveAndASalidaAsNegative() {
    var entrada =
        new MovimientoAlimento(
            UUID.randomUUID(),
            UUID.randomUUID(),
            TipoMovimientoInventario.ENTRADA,
            new BigDecimal("12"),
            EstadoMovimientoInventario.CONFIRMADO,
            Instant.now(),
            "admin");
    var salida =
        new MovimientoAlimento(
            UUID.randomUUID(),
            UUID.randomUUID(),
            TipoMovimientoInventario.SALIDA,
            new BigDecimal("5"),
            EstadoMovimientoInventario.CONFIRMADO,
            Instant.now(),
            "admin");
    assertThat(entrada.efectoEnSaldo()).isEqualByComparingTo("12");
    assertThat(salida.efectoEnSaldo()).isEqualByComparingTo("-5");
  }
}
