package com.avicontrol.domain.model.medicamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.avicontrol.domain.exception.medicamento.CantidadMedicamentoInvalidaException;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DetalleConsumoMedicamentoTest {
  @Test
  void creaDetalleValidoYRechazaMonedaOCantidadInvalida() {
    UUID recepcionId = UUID.randomUUID();
    DetalleConsumoMedicamento detalle =
        new DetalleConsumoMedicamento(
            recepcionId, new BigDecimal("10"), new BigDecimal("500.00"), "COP");
    assertThat(detalle.moneda()).isEqualTo("COP");

    assertThatThrownBy(
            () ->
                new DetalleConsumoMedicamento(
                    recepcionId, new BigDecimal("10"), new BigDecimal("500"), "USD"))
        .isInstanceOf(CantidadMedicamentoInvalidaException.class);
    assertThatThrownBy(
            () ->
                new DetalleConsumoMedicamento(
                    recepcionId, BigDecimal.ZERO, new BigDecimal("500"), "COP"))
        .isInstanceOf(CantidadMedicamentoInvalidaException.class);
  }
}
