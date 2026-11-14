package com.avicontrol.domain.model.medicamento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.avicontrol.domain.exception.medicamento.CantidadMedicamentoInvalidaException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsumoMedicamentoTest {
  @Test
  void normalizaMiligramosSinRedondear() {
    assertEquals(
        0,
        new BigDecimal("0.25")
            .compareTo(UnidadConsumoMedicamento.MILIGRAMO.normalizar(new BigDecimal("250"))));
    assertThrows(
        CantidadMedicamentoInvalidaException.class,
        () -> UnidadConsumoMedicamento.MILIGRAMO.normalizar(new BigDecimal("0.01")));
  }

  @Test
  void exigeQueLosDetallesSumenElConsumo() {
    UUID id = UUID.randomUUID();
    assertThrows(
        CantidadMedicamentoInvalidaException.class,
        () ->
            new ConsumoMedicamento(
                id,
                id,
                id,
                id,
                id,
                new BigDecimal("2"),
                UnidadConsumoMedicamento.UNIDAD,
                new BigDecimal("2"),
                UnidadConsumoMedicamento.UNIDAD,
                LocalDate.now(),
                Instant.now(),
                "vet",
                id,
                List.of(
                    new DetalleConsumoMedicamento(
                        UUID.randomUUID(), new BigDecimal("1"), BigDecimal.TEN, "COP"))));
  }
}
