package com.avicontrol.domain.model.medicamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.avicontrol.domain.exception.medicamento.CantidadMedicamentoInvalidaException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class UnidadConsumoMedicamentoTest {
  @Test
  void normalizaMiligramosAGramosYValidaEscala() {
    assertThat(UnidadConsumoMedicamento.MILIGRAMO.unidadBase())
        .isEqualTo(UnidadConsumoMedicamento.GRAMO);
    assertThat(UnidadConsumoMedicamento.MILIGRAMO.normalizar(new BigDecimal("1500")))
        .isEqualByComparingTo("1.5");
    assertThat(UnidadConsumoMedicamento.MILILITRO.normalizar(new BigDecimal("25.50")))
        .isEqualByComparingTo("25.5");

    assertThatThrownBy(() -> UnidadConsumoMedicamento.GRAMO.normalizar(BigDecimal.ZERO))
        .isInstanceOf(CantidadMedicamentoInvalidaException.class);
    assertThatThrownBy(
            () -> UnidadConsumoMedicamento.MILIGRAMO.normalizar(new BigDecimal("1.23")))
        .isInstanceOf(CantidadMedicamentoInvalidaException.class);
  }
}
