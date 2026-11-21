package com.avicontrol.domain.model.lote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class EdadTest {
  @Test
  void calculaSemanasYDiasRestantesYFormateaTexto() {
    Edad unDia = Edad.deDias(1);
    assertThat(unDia.diasTotales()).isEqualTo(1);
    assertThat(unDia.semanasCompletas()).isEqualTo(0);
    assertThat(unDia.diasRestantes()).isEqualTo(1);
    assertThat(unDia.texto()).isEqualTo("1 día");

    Edad ochoDias = Edad.deDias(8);
    assertThat(ochoDias.semanasCompletas()).isEqualTo(1);
    assertThat(ochoDias.diasRestantes()).isEqualTo(1);
    assertThat(ochoDias.texto()).isEqualTo("1 semana y 1 día (8 días)");

    Edad quinceDias = Edad.deDias(15);
    assertThat(quinceDias.semanasCompletas()).isEqualTo(2);
    assertThat(quinceDias.diasRestantes()).isEqualTo(1);
    assertThat(quinceDias.texto()).isEqualTo("2 semanas y 1 día (15 días)");
  }

  @Test
  void rechazaDiasMenoresAUno() {
    assertThatThrownBy(() -> Edad.deDias(0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Edad.deDias(-3)).isInstanceOf(IllegalArgumentException.class);
  }
}
