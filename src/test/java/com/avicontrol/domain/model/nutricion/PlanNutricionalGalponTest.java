package com.avicontrol.domain.model.nutricion;

import static org.assertj.core.api.Assertions.assertThat;

import com.avicontrol.domain.model.catalogo.EtapaCrianza;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlanNutricionalGalponTest {
  @Test
  void calculaDiasInclusivosDeEtapa() {
    PlanNutricionalGalpon.Etapa etapa =
        new PlanNutricionalGalpon.Etapa(
            UUID.randomUUID(),
            EtapaCrianza.INICIO,
            "ACTIVA",
            8,
            21,
            new BigDecimal("0.0850"),
            EtapaCrianza.INICIO,
            UUID.randomUUID(),
            new BigDecimal("40"));

    assertThat(etapa.dias()).isEqualTo(14);
  }
}
