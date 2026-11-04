package com.avicontrol.domain.model.recepcion;

import com.avicontrol.domain.exception.BusinessRuleViolationException;
import com.avicontrol.domain.model.catalogo.TipoAlimento;
import java.math.*;
import java.time.LocalDate;
import java.util.UUID;

public record RecepcionAlimento(
    UUID id,
    UUID alimentoId,
    String nombreAlimentoHistorico,
    TipoAlimento tipoAlimentoHistorico,
    String codigoLote,
    LocalDate fechaIngreso,
    LocalDate fechaVencimiento,
    BigDecimal cantidadBultos,
    BigDecimal pesoPorBultoKg,
    BigDecimal precioNetoPorBulto,
    BigDecimal porcentajeImpuesto,
    EstadoRecepcion estado) {
  public RecepcionAlimento {
    if (id == null
        || alimentoId == null
        || nombreAlimentoHistorico == null
        || nombreAlimentoHistorico.isBlank()
        || tipoAlimentoHistorico == null
        || codigoLote == null
        || codigoLote.isBlank()
        || fechaIngreso == null
        || fechaVencimiento == null
        || estado == null)
      throw new BusinessRuleViolationException(
          "RECEPCION_INVALIDA", "Faltan datos obligatorios de la recepción");
    if (!fechaVencimiento.isAfter(fechaIngreso))
      throw new BusinessRuleViolationException(
          "FECHA_VENCIMIENTO_INVALIDA", "El vencimiento debe ser posterior al ingreso");
    validar(cantidadBultos, "cantidad de bultos");
    validar(pesoPorBultoKg, "peso por bulto");
    validar(precioNetoPorBulto, "precio neto");
    if (porcentajeImpuesto == null || porcentajeImpuesto.signum() < 0)
      throw new BusinessRuleViolationException(
          "CANTIDAD_INVALIDA", "El impuesto no puede ser negativo");
  }

  private static void validar(BigDecimal n, String campo) {
    if (n == null || n.signum() <= 0)
      throw new BusinessRuleViolationException(
          "CANTIDAD_INVALIDA", "La " + campo + " debe ser positiva");
  }

  public BigDecimal pesoTotalKg() {
    return cantidadBultos.multiply(pesoPorBultoKg).setScale(4, RoundingMode.HALF_UP);
  }

  public BigDecimal precioNetoPorKg() {
    return precioNetoPorBulto.divide(pesoPorBultoKg, 4, RoundingMode.HALF_UP);
  }

  public BigDecimal subtotalNeto() {
    return cantidadBultos.multiply(precioNetoPorBulto).setScale(2, RoundingMode.HALF_UP);
  }

  public BigDecimal valorImpuesto() {
    return subtotalNeto()
        .multiply(porcentajeImpuesto)
        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
  }

  public BigDecimal totalCompra() {
    return subtotalNeto().add(valorImpuesto());
  }
}
