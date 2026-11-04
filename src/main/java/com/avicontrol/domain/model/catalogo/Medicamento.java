package com.avicontrol.domain.model.catalogo;

import com.avicontrol.domain.exception.BusinessRuleViolationException;
import java.util.UUID;

public record Medicamento(
    UUID id,
    String nombreComercial,
    String presentacionCompatible,
    UnidadMedidaMedicamento unidadBase,
    EstadoProductoCatalogo estado) {
  public Medicamento {
    if (id == null
        || nombreComercial == null
        || nombreComercial.isBlank()
        || presentacionCompatible == null
        || presentacionCompatible.isBlank()
        || unidadBase == null
        || estado == null)
      throw new BusinessRuleViolationException(
          "PRODUCTO_INVALIDO", "Los datos del medicamento son obligatorios");
  }
}
