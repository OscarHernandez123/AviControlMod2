package com.avicontrol.infrastructure.adapter.out.persistence.recepcion;

import com.avicontrol.domain.model.catalogo.TipoAlimento;
import com.avicontrol.domain.model.recepcion.*;
import jakarta.persistence.*;
import java.math.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "recepcion_alimento")
class RecepcionAlimentoJpaEntity {
  @Id UUID id;
  UUID alimentoId;
  String nombreAlimentoHistorico;

  @Enumerated(EnumType.STRING)
  TipoAlimento tipoAlimentoHistorico;

  String codigoLote;
  LocalDate fechaIngreso;
  LocalDate fechaVencimiento;
  BigDecimal cantidadBultos,
      pesoPorBultoKg,
      precioNetoPorBulto,
      porcentajeImpuesto,
      pesoTotalKg,
      precioNetoPorKg,
      subtotalNeto,
      valorImpuesto,
      totalCompra;

  @Enumerated(EnumType.STRING)
  EstadoRecepcion estado;

  protected RecepcionAlimentoJpaEntity() {}

  RecepcionAlimentoJpaEntity(RecepcionAlimento r) {
    id = r.id();
    alimentoId = r.alimentoId();
    nombreAlimentoHistorico = r.nombreAlimentoHistorico();
    tipoAlimentoHistorico = r.tipoAlimentoHistorico();
    codigoLote = r.codigoLote();
    fechaIngreso = r.fechaIngreso();
    fechaVencimiento = r.fechaVencimiento();
    cantidadBultos = r.cantidadBultos();
    pesoPorBultoKg = r.pesoPorBultoKg();
    precioNetoPorBulto = r.precioNetoPorBulto();
    porcentajeImpuesto = r.porcentajeImpuesto();
    pesoTotalKg = r.pesoTotalKg();
    precioNetoPorKg = r.precioNetoPorKg();
    subtotalNeto = r.subtotalNeto();
    valorImpuesto = r.valorImpuesto();
    totalCompra = r.totalCompra();
    estado = r.estado();
  }
}
