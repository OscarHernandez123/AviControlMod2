package com.avicontrol.domain.model.alimentacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AlertaAlimentacionTest {
  @Test
  void validaCamposNoVacios() {
    AlertaAlimentacion alerta = new AlertaAlimentacion("COBERTURA_BAJA", "Stock insuficiente");
    assertThat(alerta.tipo()).isEqualTo("COBERTURA_BAJA");
    assertThat(alerta.mensaje()).isEqualTo("Stock insuficiente");

    assertThatThrownBy(() -> new AlertaAlimentacion(" ", "msg"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new AlertaAlimentacion("TIPO", ""))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
