package com.avicontrol.domain.model.recepcion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.avicontrol.domain.exception.recepcion.CantidadInvalidaException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RecepcionCalculosTest {
  @Test
  void redondeaMonedaYPrecioUnidadConHalfUp() {
    assertThat(RecepcionCalculos.moneda(new BigDecimal("1234.565")))
        .isEqualByComparingTo("1234.57");
    assertThat(RecepcionCalculos.moneda(new BigDecimal("1234.564")))
        .isEqualByComparingTo("1234.56");
    assertThat(RecepcionCalculos.precioUnidad(new BigDecimal("100.00"), new BigDecimal("3")))
        .isEqualByComparingTo("33.3333");
  }

  @Test
  void aceptaPrecisionValidaYRechazaExcesoDeDecimalesOEnteros() {
    assertThat(RecepcionCalculos.cantidad(new BigDecimal("12.3456"), "cantidad"))
        .isEqualByComparingTo("12.3456");
    assertThat(RecepcionCalculos.dineroEntrada(new BigDecimal("999999.99"), "precio"))
        .isEqualByComparingTo("999999.99");
    assertThat(RecepcionCalculos.porcentaje(new BigDecimal("19.5000")))
        .isEqualByComparingTo("19.5");

    assertThatThrownBy(() -> RecepcionCalculos.cantidad(new BigDecimal("1.12345"), "cantidad"))
        .isInstanceOf(CantidadInvalidaException.class)
        .hasMessageContaining("cantidad");
    assertThatThrownBy(() -> RecepcionCalculos.dineroEntrada(new BigDecimal("10.123"), "precio"))
        .isInstanceOf(CantidadInvalidaException.class)
        .hasMessageContaining("precio");
    assertThatThrownBy(() -> RecepcionCalculos.porcentaje(new BigDecimal("1000.0")))
        .isInstanceOf(CantidadInvalidaException.class)
        .hasMessageContaining("impuesto");
  }
}
