package com.avicontrol.domain.model.recepcion;

import com.avicontrol.domain.exception.recepcion.CantidadInvalidaException;
import com.avicontrol.domain.exception.recepcion.FechaVencimientoInvalidaException;
import com.avicontrol.domain.model.catalogo.EtapaCrianza;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RecepcionAlimento(
    UUID id,
    UUID alimentoId,
    String nombreAlimento,
    EtapaCrianza tipoAlimento,
    String codigoLote,
    LocalDate fechaIngreso,
    LocalDate fechaVencimiento,
    int cantidadBultos,
    BigDecimal pesoPorBultoKg,
    BigDecimal precioNetoPorBulto,
    BigDecimal impuestoPorcentaje,
    BigDecimal cantidadTotalKg,
    BigDecimal precioNetoKg,
    BigDecimal precioNetoTotal,
    BigDecimal valorImpuesto,
    BigDecimal precioTotal,
    EstadoRecepcion estado,
    String actor) {

  public static RecepcionAlimento crear(
      UUID alimentoId,
      String nombreAlimento,
      EtapaCrianza tipoAlimento,
      String codigoLote,
      LocalDate ingreso,
      LocalDate vencimiento,
      int bultos,
      BigDecimal peso,
      BigDecimal precio,
      BigDecimal impuesto,
      String actor) {
    return calcular(
        UUID.randomUUID(),
        alimentoId,
        nombreAlimento,
        tipoAlimento,
        codigoLote,
        ingreso,
        vencimiento,
        bultos,
        peso,
        precio,
        impuesto,
        actor);
  }

  public RecepcionAlimento editar(
      UUID alimentoId,
      String nombreAlimento,
      LocalDate vencimiento,
      int bultos,
      BigDecimal peso,
      BigDecimal precio,
      BigDecimal impuesto) {
    return calcular(
        id,
        alimentoId,
        nombreAlimento,
        tipoAlimento,
        codigoLote,
        fechaIngreso,
        vencimiento,
        bultos,
        peso,
        precio,
        impuesto,
        actor);
  }

  private static RecepcionAlimento calcular(
      UUID id,
      UUID alimentoId,
      String nombreAlimento,
      EtapaCrianza tipoAlimento,
      String codigoLote,
      LocalDate ingreso,
      LocalDate vencimiento,
      int bultos,
      BigDecimal peso,
      BigDecimal precio,
      BigDecimal impuesto,
      String actor) {
    if (alimentoId == null
        || nombreAlimento == null
        || nombreAlimento.isBlank()
        || tipoAlimento == null
        || ingreso == null
        || vencimiento == null
        || codigoLote == null
        || codigoLote.isBlank()
        || actor == null
        || actor.isBlank()) {
      throw new CantidadInvalidaException("Datos obligatorios de recepción incompletos");
    }
    if (!vencimiento.isAfter(ingreso)) throw new FechaVencimientoInvalidaException();
    if (bultos <= 0
        || noPositivo(peso)
        || noPositivo(precio)
        || impuesto == null
        || impuesto.signum() < 0
        || impuesto.compareTo(new BigDecimal("100")) > 0) {
      throw new CantidadInvalidaException("Cantidad, peso, precio o impuesto inválido");
    }
    RecepcionCalculos.cantidad(peso, "peso por bulto");
    RecepcionCalculos.dineroEntrada(precio, "precio por bulto");
    RecepcionCalculos.porcentaje(impuesto);
    BigDecimal kg =
        RecepcionCalculos.cantidad(peso.multiply(BigDecimal.valueOf(bultos)), "peso total");
    BigDecimal subtotal = RecepcionCalculos.moneda(precio.multiply(BigDecimal.valueOf(bultos)));
    BigDecimal iva = RecepcionCalculos.moneda(subtotal.multiply(impuesto).movePointLeft(2));
    return new RecepcionAlimento(
        id,
        alimentoId,
        nombreAlimento.trim(),
        tipoAlimento,
        codigoLote.trim(),
        ingreso,
        vencimiento,
        bultos,
        peso,
        precio,
        impuesto,
        kg,
        RecepcionCalculos.precioUnidad(precio, peso),
        subtotal,
        iva,
        RecepcionCalculos.moneda(subtotal.add(iva)),
        EstadoRecepcion.CONFIRMADA,
        actor);
  }

  private static boolean noPositivo(BigDecimal value) {
    return value == null || value.signum() <= 0;
  }
}
