package com.avicontrol.domain.model.catalogo;

import com.avicontrol.domain.exception.BusinessRuleViolationException;
import java.math.BigDecimal;
import java.util.UUID;

public record Alimento(
    UUID id,
    String nombreComercial,
    String marca,
    TipoAlimento tipo,
    BigDecimal pesoNominalBultoKg,
    Integer duracionRecomendadaEtapaDias,
    EstadoProductoCatalogo estado) {
  public Alimento {
    if (id == null
        || nombreComercial == null
        || nombreComercial.isBlank()
        || marca == null
        || marca.isBlank()
        || tipo == null
        || estado == null)
      throw new BusinessRuleViolationException(
          "PRODUCTO_INVALIDO", "Los datos del alimento son obligatorios");
    if (pesoNominalBultoKg == null || pesoNominalBultoKg.signum() <= 0)
      throw new BusinessRuleViolationException(
          "PRODUCTO_INVALIDO", "El peso nominal debe ser positivo");
    if (duracionRecomendadaEtapaDias != null && duracionRecomendadaEtapaDias <= 0)
      throw new BusinessRuleViolationException(
          "PRODUCTO_INVALIDO", "La duración recomendada debe ser positiva");
  }
}
