package com.avicontrol.domain.model.lote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.avicontrol.domain.exception.galpon.FechaIngresoInconsistenteException;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LoteTest {
  @Test
  void calculaEdadInclusivaYSemanas() {
    var lote = new Lote(UUID.randomUUID(), UUID.randomUUID(), 8000, LocalDate.of(2024, 2, 29));
    Edad edad = lote.calcularEdad(LocalDate.of(2024, 3, 16));
    assertEquals(17, edad.diasTotales());
    assertEquals(2, edad.semanasCompletas());
    assertEquals(3, edad.diasRestantes());
    assertEquals("2 semanas y 3 días (17 días)", edad.texto());
    assertEquals("1 día", lote.calcularEdad(LocalDate.of(2024, 2, 29)).texto());
  }

  @Test
  void rechazaFechaFutura() {
    var lote = new Lote(UUID.randomUUID(), UUID.randomUUID(), 1, LocalDate.of(2024, 3, 1));
    assertThrows(
        FechaIngresoInconsistenteException.class,
        () -> lote.calcularEdad(LocalDate.of(2024, 2, 29)));
  }
}
