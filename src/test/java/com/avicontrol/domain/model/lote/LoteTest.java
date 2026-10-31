package com.avicontrol.domain.model.lote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.avicontrol.domain.exception.galpon.FechaIngresoInconsistenteException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LoteTest {

  @Test
  void calculatesAgeIncludingTheEntryDateAcrossCalendarBoundaries() {
    Lote lote = lote(LocalDate.of(2026, 2, 14));

    Edad edad = lote.calcularEdad(LocalDate.of(2026, 3, 2));

    assertThat(edad.diasTotales()).isEqualTo(17);
    assertThat(edad.semanasCompletas()).isEqualTo(2);
    assertThat(edad.diasRestantes()).isEqualTo(3);
    assertThat(edad.texto()).isEqualTo("2 semanas y 3 días (17 días)");
  }

  @Test
  void returnsOneDayOnTheEntryDateAndRejectsAFutureEntryDate() {
    Lote lote = lote(LocalDate.of(2026, 10, 31));

    assertThat(lote.calcularEdad(LocalDate.of(2026, 10, 31)).texto()).isEqualTo("1 día");
    assertThatThrownBy(() -> lote.calcularEdad(LocalDate.of(2026, 10, 30)))
        .isInstanceOf(FechaIngresoInconsistenteException.class);
  }

  private Lote lote(LocalDate fechaIngreso) {
    return new Lote(
        UUID.randomUUID(), "Lote A", 8000, 7960, fechaIngreso, BigDecimal.TEN, UUID.randomUUID());
  }
}
