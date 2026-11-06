package com.avicontrol.domain.model.sanitario.enfermedad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.avicontrol.domain.exception.sanitario.enfermedad.DatosEnfermedadInvalidosException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EnfermedadTest {
  @Test
  void exigeDecisionExplicitaYTextos() {
    Instant now = Instant.now();
    assertThrows(
        DatosEnfermedadInvalidosException.class,
        () ->
            new Enfermedad(
                UUID.randomUUID(),
                "Newcastle",
                NivelRiesgo.ALTO,
                TipoEnfermedad.VIRAL,
                "Clínica",
                null,
                0,
                now,
                now));
    assertThrows(
        DatosEnfermedadInvalidosException.class,
        () ->
            new Enfermedad(
                UUID.randomUUID(),
                "  ",
                NivelRiesgo.ALTO,
                TipoEnfermedad.VIRAL,
                "Clínica",
                false,
                0,
                now,
                now));
  }

  @Test
  void edicionConservaIdentidadEIncrementaVersion() {
    Instant now = Instant.now();
    Enfermedad original =
        new Enfermedad(
            UUID.randomUUID(),
            "Newcastle",
            NivelRiesgo.ALTO,
            TipoEnfermedad.VIRAL,
            "Clínica",
            false,
            0,
            now,
            now);
    Enfermedad edited =
        original.actualizar(
            "Newcastle",
            NivelRiesgo.CRITICO,
            TipoEnfermedad.VIRAL,
            "Nueva clínica",
            true,
            now.plusSeconds(1));
    assertEquals(original.id(), edited.id());
    assertEquals(1, edited.version());
    assertEquals(false, original.requiereSacrificioSanitario());
    assertEquals(true, edited.requiereSacrificioSanitario());
  }
}
