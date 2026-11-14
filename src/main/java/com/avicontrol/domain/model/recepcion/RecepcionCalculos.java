package com.avicontrol.domain.model.recepcion;

import com.avicontrol.domain.exception.recepcion.CantidadInvalidaException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class RecepcionCalculos {
  private RecepcionCalculos() {}

  public static BigDecimal cantidad(BigDecimal valor, String campo) {
    validarPrecision(valor, 14, 4, campo);
    return valor;
  }

  public static BigDecimal dineroEntrada(BigDecimal valor, String campo) {
    validarPrecision(valor, 16, 2, campo);
    return valor;
  }

  public static BigDecimal porcentaje(BigDecimal valor) {
    validarPrecision(valor, 3, 4, "impuesto");
    return valor;
  }

  public static BigDecimal moneda(BigDecimal valor) {
    BigDecimal redondeado = valor.setScale(2, RoundingMode.HALF_UP);
    validarPrecision(redondeado, 16, 2, "valor monetario");
    return redondeado;
  }

  public static BigDecimal precioUnidad(BigDecimal precio, BigDecimal contenido) {
    BigDecimal resultado = precio.divide(contenido, 4, RoundingMode.HALF_UP);
    validarPrecision(resultado, 14, 4, "precio por unidad");
    return resultado;
  }

  private static void validarPrecision(BigDecimal valor, int enteros, int decimales, String campo) {
    BigDecimal sinCeros = valor.stripTrailingZeros();
    if (Math.max(0, sinCeros.scale()) > decimales
        || sinCeros.precision() - sinCeros.scale() > enteros) {
      throw new CantidadInvalidaException(campo + " excede la precisión permitida");
    }
  }
}
