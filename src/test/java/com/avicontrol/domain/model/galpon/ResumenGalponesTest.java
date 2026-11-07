package com.avicontrol.domain.model.galpon;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ResumenGalponesTest {
  @Test
  void cuentaSeisEstadosYRegistrosInconsistentes() {
    var resumen =
        ResumenGalpones.contar(
            Arrays.asList(
                "DISPONIBLE",
                "VACIADO_SANITARIO",
                "PRODUCTIVA",
                "EN_COSECHA",
                "MANTENIMIENTO",
                "AISLAMIENTO",
                "OTRO",
                null));
    assertEquals(8, resumen.totalRegistrado());
    assertEquals(6, resumen.totalValido());
    assertEquals(2, resumen.registrosInconsistentes());
    for (EstadoGalpon estado : EstadoGalpon.values())
      assertEquals(1, resumen.porEstado().get(estado));
    assertEquals(0, ResumenGalpones.contar(java.util.List.of()).totalRegistrado());
  }
}
