package com.avicontrol.domain.model.medicamento;

import com.avicontrol.domain.exception.medicamento.CantidadMedicamentoInvalidaException;
import java.math.BigDecimal;

public enum UnidadConsumoMedicamento {
  MILIGRAMO,
  GRAMO,
  MILILITRO,
  UNIDAD;

  public UnidadConsumoMedicamento unidadBase() {
    return this == MILIGRAMO ? GRAMO : this;
  }

  public BigDecimal normalizar(BigDecimal cantidad) {
    if (cantidad == null || cantidad.signum() <= 0)
      throw new CantidadMedicamentoInvalidaException("La cantidad debe ser positiva");
    BigDecimal normalizada = this == MILIGRAMO ? cantidad.movePointLeft(3) : cantidad;
    if (cantidad.stripTrailingZeros().precision() - cantidad.stripTrailingZeros().scale() > 14
        || normalizada.stripTrailingZeros().scale() > 4
        || normalizada.precision() - normalizada.scale() > 14)
      throw new CantidadMedicamentoInvalidaException(
          "La cantidad no cabe en la unidad base sin redondeo");
    return normalizada.stripTrailingZeros();
  }
}
