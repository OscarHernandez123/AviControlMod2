package com.avicontrol.domain.model.inventario;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InventarioActualTest {
  @Test
  void constructorSecundarioInicializaTarjetasMedicamentoVacias() {
    ExistenciaAlimento alimento =
        new ExistenciaAlimento(
            UUID.randomUUID(),
            "Preiniciador",
            "PREINICIO",
            2,
            LocalDate.of(2026, 12, 31),
            new BigDecimal("500.00"),
            new BigDecimal("120.00"),
            "SUFICIENTE");
    ExistenciaMedicamento medicamento =
        new ExistenciaMedicamento(
            UUID.randomUUID(),
            "Enrofloxacina",
            "Frasco 100 ml",
            new BigDecimal("5"),
            new BigDecimal("100"),
            "ML",
            new BigDecimal("500"));

    InventarioActual actual =
        new InventarioActual(
            List.of(alimento), List.of(medicamento), Map.of("TOTAL_KG", new BigDecimal("500.00")));

    assertThat(actual.alimentos()).hasSize(1);
    assertThat(actual.medicamentos()).hasSize(1);
    assertThat(actual.tarjetasAlimentoKg()).containsEntry("TOTAL_KG", new BigDecimal("500.00"));
    assertThat(actual.tarjetasMedicamento()).isEmpty();
  }
}
