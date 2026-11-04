package com.avicontrol.domain.model.recepcion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.avicontrol.domain.exception.BusinessRuleViolationException;
import com.avicontrol.domain.model.catalogo.TipoAlimento;
import com.avicontrol.domain.model.catalogo.UnidadMedidaMedicamento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecepcionTest {
  @Test
  void calculaImportesDeAlimentoConRedondeoUniforme() {
    RecepcionAlimento recepcion =
        new RecepcionAlimento(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Inicio",
            TipoAlimento.INICIO,
            "L-1",
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 2, 1),
            new BigDecimal("3"),
            new BigDecimal("40"),
            new BigDecimal("125000"),
            new BigDecimal("5"),
            EstadoRecepcion.CONFIRMADA);

    assertThat(recepcion.pesoTotalKg()).isEqualByComparingTo("120.0000");
    assertThat(recepcion.precioNetoPorKg()).isEqualByComparingTo("3125.0000");
    assertThat(recepcion.totalCompra()).isEqualByComparingTo("393750.00");
  }

  @Test
  void normalizaMiligramosAGramosParaInventario() {
    RecepcionMedicamento recepcion =
        new RecepcionMedicamento(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Antibiótico",
            "M-1",
            "Frasco",
            new BigDecimal("2"),
            new BigDecimal("500"),
            UnidadMedidaMedicamento.MILIGRAMO,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 2, 1),
            new BigDecimal("10000"),
            new BigDecimal("19"),
            EstadoRecepcion.CONFIRMADA);

    assertThat(recepcion.unidadBase()).isEqualTo(UnidadMedidaMedicamento.GRAMO);
    assertThat(recepcion.contenidoTotalBase()).isEqualByComparingTo("1.0000");
    assertThat(recepcion.precioNetoPorUnidadBase()).isEqualByComparingTo("20000.0000");
  }

  @Test
  void rechazaVencimientoNoPosteriorAlIngreso() {
    assertThatThrownBy(
            () ->
                new RecepcionAlimento(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "Inicio",
                    TipoAlimento.INICIO,
                    "L-1",
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 1, 1),
                    BigDecimal.ONE,
                    BigDecimal.ONE,
                    BigDecimal.ONE,
                    BigDecimal.ZERO,
                    EstadoRecepcion.CONFIRMADA))
        .isInstanceOf(BusinessRuleViolationException.class)
        .hasMessageContaining("vencimiento");
  }
}
