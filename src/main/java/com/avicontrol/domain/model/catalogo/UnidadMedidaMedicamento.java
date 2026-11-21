package com.avicontrol.domain.model.catalogo;

import com.avicontrol.domain.exception.recepcion.UnidadIncompatibleException;
import java.math.BigDecimal;

public enum UnidadMedidaMedicamento {
  MILIGRAMO,
  GRAMO,
  MILILITRO,
  UNIDAD;

  public UnidadMedidaMedicamento unidadBase() {
    return this == MILIGRAMO ? GRAMO : this;
  }

  public BigDecimal normalizar(BigDecimal cantidad) {
    return this == MILIGRAMO ? cantidad.movePointLeft(3) : cantidad;
  }

  public void validarCompatibleCon(UnidadMedidaMedicamento unidadBase) {
    if (unidadBase() != unidadBase) {
      throw new UnidadIncompatibleException();
    }
  }
}
