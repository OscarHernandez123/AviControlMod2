package com.avicontrol.domain.model.nutricion;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.avicontrol.domain.model.catalogo.EtapaCrianza;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlanNutricionalPlantillaTest {
  @Test
  void rechazaSolapamientosYHuecosEntreEtapas() {
    assertThrows(IllegalArgumentException.class, () -> plantilla(7, 7, "0.05"));
    assertThrows(IllegalArgumentException.class, () -> plantilla(7, 9, "0.05"));
  }

  @Test
  void rechazaRacionNoPositiva() {
    assertThrows(IllegalArgumentException.class, () -> plantilla(7, 8, "0"));
    assertThrows(IllegalArgumentException.class, () -> plantilla(7, 8, "-0.02"));
  }

  private PlanNutricionalPlantilla plantilla(int finPrimera, int inicioSegunda, String racion) {
    return new PlanNutricionalPlantilla(
        null,
        "Broiler",
        "BROILER",
        true,
        true,
        Instant.EPOCH,
        List.of(
            etapa(EtapaCrianza.PRE_INICIO, 1, finPrimera, "0.05"),
            etapa(EtapaCrianza.INICIO, inicioSegunda, 21, racion),
            etapa(EtapaCrianza.ENGORDE, 22, 45, "0.12")));
  }

  private PlanNutricionalPlantilla.Etapa etapa(
      EtapaCrianza tipo, int inicio, int fin, String racion) {
    return new PlanNutricionalPlantilla.Etapa(
        tipo, inicio, fin, new BigDecimal(racion), tipo, new BigDecimal("40"), null);
  }
}
