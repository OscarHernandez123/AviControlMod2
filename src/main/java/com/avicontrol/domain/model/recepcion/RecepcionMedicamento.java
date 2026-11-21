package com.avicontrol.domain.model.recepcion;

import com.avicontrol.domain.exception.recepcion.CantidadInvalidaException;
import com.avicontrol.domain.exception.recepcion.FechaVencimientoInvalidaException;
import com.avicontrol.domain.model.catalogo.UnidadMedidaMedicamento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RecepcionMedicamento(
    UUID id,
    UUID medicamentoId,
    String nombreMedicamento,
    String codigoLote,
    String presentacion,
    LocalDate fechaIngreso,
    LocalDate fechaVencimiento,
    int cantidadPresentaciones,
    BigDecimal contenidoPorPresentacion,
    UnidadMedidaMedicamento unidadContenido,
    BigDecimal precioNetoPorPresentacion,
    BigDecimal impuestoPorcentaje,
    BigDecimal cantidadUnidadBase,
    UnidadMedidaMedicamento unidadBase,
    BigDecimal precioNetoUnidadBase,
    BigDecimal precioNetoTotal,
    BigDecimal valorImpuesto,
    BigDecimal precioTotal,
    EstadoRecepcion estado,
    String actor) {

  public static RecepcionMedicamento crear(
      UUID medicamentoId,
      String nombreMedicamento,
      String codigoLote,
      String presentacion,
      LocalDate ingreso,
      LocalDate vencimiento,
      int cantidad,
      BigDecimal contenido,
      UnidadMedidaMedicamento unidad,
      BigDecimal precio,
      BigDecimal impuesto,
      String actor) {
    return calcular(
        UUID.randomUUID(),
        medicamentoId,
        nombreMedicamento,
        codigoLote,
        presentacion,
        ingreso,
        vencimiento,
        cantidad,
        contenido,
        unidad,
        precio,
        impuesto,
        actor);
  }

  public RecepcionMedicamento editar(
      UUID medicamentoId,
      String nombreMedicamento,
      LocalDate vencimiento,
      int cantidad,
      BigDecimal contenido,
      UnidadMedidaMedicamento unidad,
      BigDecimal precio,
      BigDecimal impuesto) {
    return calcular(
        id,
        medicamentoId,
        nombreMedicamento,
        codigoLote,
        presentacion,
        fechaIngreso,
        vencimiento,
        cantidad,
        contenido,
        unidad,
        precio,
        impuesto,
        actor);
  }

  private static RecepcionMedicamento calcular(
      UUID id,
      UUID medicamentoId,
      String nombreMedicamento,
      String codigoLote,
      String presentacion,
      LocalDate ingreso,
      LocalDate vencimiento,
      int cantidad,
      BigDecimal contenido,
      UnidadMedidaMedicamento unidad,
      BigDecimal precio,
      BigDecimal impuesto,
      String actor) {
    if (medicamentoId == null
        || nombreMedicamento == null
        || nombreMedicamento.isBlank()
        || ingreso == null
        || vencimiento == null
        || unidad == null
        || codigoLote == null
        || codigoLote.isBlank()
        || presentacion == null
        || presentacion.isBlank()
        || actor == null
        || actor.isBlank()) {
      throw new CantidadInvalidaException("Datos obligatorios de recepción incompletos");
    }
    if (!vencimiento.isAfter(ingreso)) throw new FechaVencimientoInvalidaException();
    if (cantidad <= 0
        || noPositivo(contenido)
        || noPositivo(precio)
        || impuesto == null
        || impuesto.signum() < 0
        || impuesto.compareTo(new BigDecimal("100")) > 0) {
      throw new CantidadInvalidaException("Cantidad, contenido, precio o impuesto inválido");
    }
    RecepcionCalculos.cantidad(contenido, "contenido por presentación");
    RecepcionCalculos.dineroEntrada(precio, "precio por presentación");
    RecepcionCalculos.porcentaje(impuesto);
    BigDecimal base = unidad.normalizar(contenido);
    BigDecimal totalBase =
        RecepcionCalculos.cantidad(base.multiply(BigDecimal.valueOf(cantidad)), "contenido total");
    BigDecimal subtotal = RecepcionCalculos.moneda(precio.multiply(BigDecimal.valueOf(cantidad)));
    BigDecimal iva = RecepcionCalculos.moneda(subtotal.multiply(impuesto).movePointLeft(2));
    return new RecepcionMedicamento(
        id,
        medicamentoId,
        nombreMedicamento.trim(),
        codigoLote.trim(),
        presentacion.trim(),
        ingreso,
        vencimiento,
        cantidad,
        contenido,
        unidad,
        precio,
        impuesto,
        totalBase,
        unidad.unidadBase(),
        RecepcionCalculos.precioUnidad(precio, base),
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
