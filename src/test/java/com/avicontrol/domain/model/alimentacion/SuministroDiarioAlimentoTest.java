package com.avicontrol.domain.model.alimentacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SuministroDiarioAlimentoTest {
  @Test
  void creaSuministroValidoConDetallesFefoExactos() {
    UUID r1 = UUID.randomUUID();
    UUID r2 = UUID.randomUUID();
    SuministroDiarioAlimento suministro =
        new SuministroDiarioAlimento(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "INICIO",
            new BigDecimal("50.0000"),
            LocalDate.of(2026, 11, 20),
            Instant.now(),
            "trabajador",
            UUID.randomUUID(),
            List.of(
                new SuministroDiarioAlimento.Detalle(
                    r1, new BigDecimal("30.0000"), new BigDecimal("2500.00")),
                new SuministroDiarioAlimento.Detalle(
                    r2, new BigDecimal("20.0000"), new BigDecimal("2600.00"))));

    assertThat(suministro.detalles()).hasSize(2);
    assertThat(suministro.cantidadKg()).isEqualByComparingTo("50");
  }

  @Test
  void rechazaCuandoDetalleFefoNoCubreTotalOEscalaExcedeCuatro() {
    UUID r1 = UUID.randomUUID();
    assertThatThrownBy(
            () ->
                new SuministroDiarioAlimento(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "INICIO",
                    new BigDecimal("50"),
                    LocalDate.of(2026, 11, 20),
                    Instant.now(),
                    "trabajador",
                    UUID.randomUUID(),
                    List.of(
                        new SuministroDiarioAlimento.Detalle(
                            r1, new BigDecimal("40"), new BigDecimal("2500")))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("FEFO");

    assertThatThrownBy(
            () ->
                new SuministroDiarioAlimento(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "INICIO",
                    new BigDecimal("10.12345"),
                    LocalDate.of(2026, 11, 20),
                    Instant.now(),
                    "trabajador",
                    UUID.randomUUID(),
                    List.of(
                        new SuministroDiarioAlimento.Detalle(
                            r1, new BigDecimal("10.12345"), new BigDecimal("2500")))))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
