package com.avicontrol.domain.model.sacrificio.productivo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.avicontrol.domain.model.lote.Edad;
import org.junit.jupiter.api.Test;

class ValidacionAptitudSacrificioTest {
  @Test
  void evaluaAmbasCondicionesSinPerderMotivos() {
    assertTrue(ValidacionAptitudSacrificio.evaluar("PRODUCTIVO", Edad.deDias(45)).apto());
    assertFalse(ValidacionAptitudSacrificio.evaluar("PRODUCTIVA", Edad.deDias(44)).apto());
    assertFalse(ValidacionAptitudSacrificio.evaluar("MANTENIMIENTO", Edad.deDias(45)).apto());
    var ambos = ValidacionAptitudSacrificio.evaluar("MANTENIMIENTO", Edad.deDias(44));
    assertEquals(2, ambos.motivosNoAptitud().size());
  }
}
