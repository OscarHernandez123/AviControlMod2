package com.avicontrol.domain.model.recepcion;

import com.avicontrol.domain.exception.BusinessRuleViolationException;
import com.avicontrol.domain.model.catalogo.UnidadMedidaMedicamento;
import java.math.*;
import java.time.LocalDate;
import java.util.UUID;

public record RecepcionMedicamento(
    UUID id,
    UUID medicamentoId,
    String nombreMedicamentoHistorico,
    String codigoLote,
    String presentacion,
    BigDecimal cantidad,
    BigDecimal contenidoPorPresentacion,
    UnidadMedidaMedicamento unidadMedida,
    LocalDate fechaIngreso,
    LocalDate fechaVencimiento,
    BigDecimal precioNetoPorPresentacion,
    BigDecimal porcentajeImpuesto,
    EstadoRecepcion estado) {
  public RecepcionMedicamento {
    if (id == null
        || medicamentoId == null
        || nombreMedicamentoHistorico == null
        || nombreMedicamentoHistorico.isBlank()
        || codigoLote == null
        || codigoLote.isBlank()
        || presentacion == null
        || presentacion.isBlank()
        || unidadMedida == null
        || fechaIngreso == null
        || fechaVencimiento == null
        || estado == null)
      throw new BusinessRuleViolationException(
          "RECEPCION_INVALIDA", "Faltan datos obligatorios de la recepción");
    if (!fechaVencimiento.isAfter(fechaIngreso))
      throw new BusinessRuleViolationException(
          "FECHA_VENCIMIENTO_INVALIDA", "El vencimiento debe ser posterior al ingreso");
    v(cantidad, "cantidad");
    v(contenidoPorPresentacion, "contenido");
    v(precioNetoPorPresentacion, "precio neto");
    if (porcentajeImpuesto == null || porcentajeImpuesto.signum() < 0)
      throw new BusinessRuleViolationException(
          "CANTIDAD_INVALIDA", "El impuesto no puede ser negativo");
  }

  private static void v(BigDecimal n, String s) {
    if (n == null || n.signum() <= 0)
      throw new BusinessRuleViolationException(
          "CANTIDAD_INVALIDA", "El " + s + " debe ser positivo");
  }

  public BigDecimal contenidoTotal() {
    return cantidad.multiply(contenidoPorPresentacion).setScale(4, RoundingMode.HALF_UP);
  }

  public BigDecimal contenidoTotalBase() {
    return cantidad.multiply(contenidoPorPresentacionBase()).setScale(4, RoundingMode.HALF_UP);
  }

  public BigDecimal contenidoPorPresentacionBase() {
    return unidadMedida == UnidadMedidaMedicamento.MILIGRAMO
        ? contenidoPorPresentacion.divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP)
        : contenidoPorPresentacion.setScale(4, RoundingMode.HALF_UP);
  }

  public UnidadMedidaMedicamento unidadBase() {
    return unidadMedida == UnidadMedidaMedicamento.MILIGRAMO
        ? UnidadMedidaMedicamento.GRAMO
        : unidadMedida;
  }

  public BigDecimal precioNetoPorUnidadBase() {
    return precioNetoPorPresentacion.divide(
        contenidoPorPresentacionBase(), 4, RoundingMode.HALF_UP);
  }

  public BigDecimal subtotalNeto() {
    return cantidad.multiply(precioNetoPorPresentacion).setScale(2, RoundingMode.HALF_UP);
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
