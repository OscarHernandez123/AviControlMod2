package com.avicontrol.domain.model.sanitario.enfermedad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class EnfermedadTest {

  @Test
  void trimsValuesAndPreservesTheExplicitSacrificeDecision() {
    Enfermedad enfermedad =
        new Enfermedad(
            UUID.randomUUID(),
            " Newcastle ",
            NivelRiesgo.ALTO,
            TipoEnfermedad.VIRAL,
            " Descripción ",
            true);

    assertThat(enfermedad.nombre()).isEqualTo("Newcastle");
    assertThat(enfermedad.descripcionClinica()).isEqualTo("Descripción");
    assertThat(enfermedad.requiereSacrificioSanitario()).isTrue();
  }

  @Test
  void rejectsBlankRequiredData() {
    assertThatThrownBy(
            () ->
                new Enfermedad(
                    UUID.randomUUID(), " ", NivelRiesgo.BAJO, TipoEnfermedad.OTRA, "texto", false))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
