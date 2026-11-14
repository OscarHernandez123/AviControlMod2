package com.avicontrol.domain.model.medicamento;

import com.avicontrol.domain.exception.medicamento.CantidadMedicamentoInvalidaException;
import java.math.BigDecimal;
import java.util.UUID;

public record DetalleConsumoMedicamento(
    UUID recepcionId,
    BigDecimal cantidadUnidadBase,
    BigDecimal precioNetoHistoricoUnidadBase,
    String moneda) {
  public DetalleConsumoMedicamento {
    if (recepcionId == null
        || cantidadUnidadBase == null
        || cantidadUnidadBase.signum() <= 0
        || precioNetoHistoricoUnidadBase == null
        || precioNetoHistoricoUnidadBase.signum() < 0
        || !"COP".equals(moneda))
      throw new CantidadMedicamentoInvalidaException("Detalle de consumo inválido");
  }
}
