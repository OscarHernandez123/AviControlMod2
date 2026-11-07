package com.avicontrol.domain.model.sanitario.diagnostico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.avicontrol.domain.exception.sanitario.diagnostico.MedicacionNoCompatibleException;
import com.avicontrol.domain.exception.sanitario.diagnostico.MedicacionRequeridaException;
import com.avicontrol.domain.model.sanitario.enfermedad.Enfermedad;
import com.avicontrol.domain.model.sanitario.enfermedad.NivelRiesgo;
import com.avicontrol.domain.model.sanitario.enfermedad.TipoEnfermedad;
import com.avicontrol.domain.model.sanitario.medicacion.Medicacion;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DiagnosticoGalponTest {
  private Enfermedad enfermedad(boolean sacrificio) {
    Instant now = Instant.now();
    return new Enfermedad(
        UUID.randomUUID(),
        "Newcastle",
        NivelRiesgo.ALTO,
        TipoEnfermedad.VIRAL,
        "Signos",
        sacrificio,
        0,
        now,
        now);
  }

  private Medicacion medicacion(UUID enfermedadId, int dias) {
    Instant now = Instant.now();
    return new Medicacion(
        UUID.randomUUID(), enfermedadId, UUID.randomUUID(), "0.5 ml", dias, "Aplicar", 0, now, now);
  }

  @Test
  void tratamientoConservaDuracionYFechaDeConfirmacion() {
    Enfermedad e = enfermedad(false);
    Medicacion m = medicacion(e.id(), 5);
    LocalDate fecha = LocalDate.of(2026, 11, 6);
    DiagnosticoGalpon d =
        DiagnosticoGalpon.crear(UUID.randomUUID(), UUID.randomUUID(), e, m, "Tratamiento", fecha);
    assertEquals(fecha.plusDays(5), d.fechaReintegro());
    assertEquals(5, d.duracionDias());
    assertEquals("0.5 ml", d.dosis());
    assertEquals(
        7,
        m.actualizar(e.id(), m.medicamentoId(), "1 ml", 7, "Aplicar", Instant.now())
            .duracionDias());
    assertEquals(5, d.duracionDias());
  }

  @Test
  void sacrificioNoTieneMedicacionNiFecha() {
    Enfermedad e = enfermedad(true);
    DiagnosticoGalpon d =
        DiagnosticoGalpon.crear(
            UUID.randomUUID(), UUID.randomUUID(), e, null, "Sacrificio", LocalDate.of(2026, 11, 6));
    assertEquals(true, d.requiereSacrificioSanitario());
    assertNull(d.medicacionId());
    assertNull(d.fechaReintegro());
  }

  @Test
  void exigeMedicacionCompatible() {
    Enfermedad e = enfermedad(false);
    assertThrows(
        MedicacionRequeridaException.class,
        () ->
            DiagnosticoGalpon.crear(
                UUID.randomUUID(), UUID.randomUUID(), e, null, "Tratamiento", LocalDate.now()));
    assertThrows(
        MedicacionNoCompatibleException.class,
        () ->
            DiagnosticoGalpon.crear(
                UUID.randomUUID(),
                UUID.randomUUID(),
                e,
                medicacion(UUID.randomUUID(), 5),
                "Tratamiento",
                LocalDate.now()));
  }
}
