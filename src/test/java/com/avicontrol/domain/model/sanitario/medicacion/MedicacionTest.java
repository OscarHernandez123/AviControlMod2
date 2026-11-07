package com.avicontrol.domain.model.sanitario.medicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.avicontrol.domain.exception.sanitario.medicacion.DatosMedicacionInvalidosException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MedicacionTest {
  @Test
  void rechazaDuracionYTextosInvalidos() {
    Instant now = Instant.now();
    UUID id = UUID.randomUUID();
    assertThrows(
        DatosMedicacionInvalidosException.class,
        () -> new Medicacion(id, id, id, "dosis", 0, "descripción", 0, now, now));
    assertThrows(
        DatosMedicacionInvalidosException.class,
        () -> new Medicacion(id, id, id, "  ", 5, "descripción", 0, now, now));
  }

  @Test
  void edicionNoMutaValorAnterior() {
    Instant now = Instant.now();
    UUID id = UUID.randomUUID();
    Medicacion original = new Medicacion(id, id, id, "0.5 ml", 5, "Aplicar", 0, now, now);
    Medicacion edited = original.actualizar(id, id, "1 ml", 7, "Aplicar", now.plusSeconds(1));
    assertEquals(5, original.duracionDias());
    assertEquals(7, edited.duracionDias());
    assertEquals(1, edited.version());
  }

  @Test
  void validaDiasAlternosYCalculaDescanso() {
    Instant now = Instant.now();
    UUID id = UUID.randomUUID();
    Medicacion m =
        new Medicacion(
            id, id, id, "1 ml", 5, "Agua", 0, now, now, "Fármaco", "Principio", List.of(1, 3, 5));
    assertEquals(true, m.aplicaDia(3));
    assertEquals(false, m.aplicaDia(2));
    assertThrows(
        DatosMedicacionInvalidosException.class,
        () ->
            new Medicacion(
                id, id, id, "1 ml", 5, "Agua", 0, now, now, "Fármaco", "Principio", List.of(1, 6)));
    assertThrows(
        DatosMedicacionInvalidosException.class,
        () ->
            new Medicacion(
                id, id, id, "1 ml", 5, "Agua", 0, now, now, "Fármaco", "Principio", List.of(1, 1)));
  }
}
