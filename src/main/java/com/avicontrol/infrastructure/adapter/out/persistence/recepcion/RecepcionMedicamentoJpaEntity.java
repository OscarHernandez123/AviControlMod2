package com.avicontrol.infrastructure.adapter.out.persistence.recepcion;

import com.avicontrol.domain.model.catalogo.UnidadMedidaMedicamento;
import com.avicontrol.domain.model.recepcion.*;
import jakarta.persistence.*;
import java.math.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "recepcion_medicamento")
class RecepcionMedicamentoJpaEntity {
  @Id UUID id;
  UUID medicamentoId;
  String nombreMedicamentoHistorico, codigoLote, presentacion;
  BigDecimal cantidad,
      contenidoPorPresentacion,
      precioNetoPorPresentacion,
      porcentajeImpuesto,
      contenidoTotalBase,
      precioNetoPorUnidadBase,
      subtotalNeto,
      valorImpuesto,
      totalCompra;

  @Enumerated(EnumType.STRING)
  UnidadMedidaMedicamento unidadMedida;

  LocalDate fechaIngreso, fechaVencimiento;

  @Enumerated(EnumType.STRING)
  EstadoRecepcion estado;

  protected RecepcionMedicamentoJpaEntity() {}

  RecepcionMedicamentoJpaEntity(RecepcionMedicamento r) {
    id = r.id();
    medicamentoId = r.medicamentoId();
    nombreMedicamentoHistorico = r.nombreMedicamentoHistorico();
    codigoLote = r.codigoLote();
    presentacion = r.presentacion();
    cantidad = r.cantidad();
    contenidoPorPresentacion = r.contenidoPorPresentacion();
    unidadMedida = r.unidadMedida();
    fechaIngreso = r.fechaIngreso();
    fechaVencimiento = r.fechaVencimiento();
    precioNetoPorPresentacion = r.precioNetoPorPresentacion();
    porcentajeImpuesto = r.porcentajeImpuesto();
    contenidoTotalBase = r.contenidoTotalBase();
    precioNetoPorUnidadBase = r.precioNetoPorUnidadBase();
    subtotalNeto = r.subtotalNeto();
    valorImpuesto = r.valorImpuesto();
    totalCompra = r.totalCompra();
    estado = r.estado();
  }
}
