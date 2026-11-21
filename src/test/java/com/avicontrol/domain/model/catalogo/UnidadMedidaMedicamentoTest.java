package com.avicontrol.domain.model.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.avicontrol.domain.exception.recepcion.UnidadIncompatibleException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class UnidadMedidaMedicamentoTest {
  @Test
  void normalizaMiligramosAGramosYValidaCompatibilidad() {
    assertThat(UnidadMedidaMedicamento.MILIGRAMO.unidadBase())
        .isEqualTo(UnidadMedidaMedicamento.GRAMO);
    assertThat(UnidadMedidaMedicamento.MILIGRAMO.normalizar(new BigDecimal("2500")))
        .isEqualByComparingTo("2.500");
    assertThat(UnidadMedidaMedicamento.MILILITRO.normalizar(new BigDecimal("100")))
        .isEqualByComparingTo("100");

    assertThatNoException()
        .isThrownBy(
            () ->
                UnidadMedidaMedicamento.MILIGRAMO.validarCompatibleCon(
                    UnidadMedidaMedicamento.GRAMO));
    assertThatThrownBy(
            () ->
                UnidadMedidaMedicamento.MILILITRO.validarCompatibleCon(
                    UnidadMedidaMedicamento.GRAMO))
        .isInstanceOf(UnidadIncompatibleException.class);
  }
}
