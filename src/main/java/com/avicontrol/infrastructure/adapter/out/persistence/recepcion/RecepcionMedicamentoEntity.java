package com.avicontrol.infrastructure.adapter.out.persistence.recepcion;

import com.avicontrol.domain.model.catalogo.UnidadMedidaMedicamento;
import com.avicontrol.domain.model.recepcion.EstadoRecepcion;
import com.avicontrol.domain.model.recepcion.RecepcionMedicamento;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "recepciones_medicamento")
public class RecepcionMedicamentoEntity {
  @Id private UUID id;
  private UUID medicamentoId;
  private String nombreMedicamento;
  private String codigoLote;
  private String presentacion;
  private LocalDate fechaIngreso;
  private LocalDate fechaVencimiento;
  private int cantidadPresentaciones;
  private BigDecimal contenidoPorPresentacion;
  private String unidadContenido;
  private BigDecimal precioNetoPorPresentacion;
  private BigDecimal impuestoPorcentaje;
  private BigDecimal cantidadUnidadBase;
  private String unidadBase;
  private BigDecimal precioNetoUnidadBase;
  private BigDecimal precioNetoTotal;
  private BigDecimal valorImpuesto;
  private BigDecimal precioTotal;
  private String estado;
  private String actor;

  protected RecepcionMedicamentoEntity() {}

  public RecepcionMedicamentoEntity(RecepcionMedicamento r) {
    id = r.id();
    medicamentoId = r.medicamentoId();
    nombreMedicamento = r.nombreMedicamento();
    codigoLote = r.codigoLote();
    presentacion = r.presentacion();
    fechaIngreso = r.fechaIngreso();
    fechaVencimiento = r.fechaVencimiento();
    cantidadPresentaciones = r.cantidadPresentaciones();
    contenidoPorPresentacion = r.contenidoPorPresentacion();
    unidadContenido = r.unidadContenido().name();
    precioNetoPorPresentacion = r.precioNetoPorPresentacion();
    impuestoPorcentaje = r.impuestoPorcentaje();
    cantidadUnidadBase = r.cantidadUnidadBase();
    unidadBase = r.unidadBase().name();
    precioNetoUnidadBase = r.precioNetoUnidadBase();
    precioNetoTotal = r.precioNetoTotal();
    valorImpuesto = r.valorImpuesto();
    precioTotal = r.precioTotal();
    estado = r.estado().name();
    actor = r.actor();
  }

  public RecepcionMedicamento toDomain() {
    return new RecepcionMedicamento(
        id,
        medicamentoId,
        nombreMedicamento,
        codigoLote,
        presentacion,
        fechaIngreso,
        fechaVencimiento,
        cantidadPresentaciones,
        contenidoPorPresentacion,
        UnidadMedidaMedicamento.valueOf(unidadContenido),
        precioNetoPorPresentacion,
        impuestoPorcentaje,
        cantidadUnidadBase,
        UnidadMedidaMedicamento.valueOf(unidadBase),
        precioNetoUnidadBase,
        precioNetoTotal,
        valorImpuesto,
        precioTotal,
        EstadoRecepcion.valueOf(estado),
        actor);
  }

  public void actualizar(RecepcionMedicamento r) {
    medicamentoId = r.medicamentoId();
    nombreMedicamento = r.nombreMedicamento();
    fechaVencimiento = r.fechaVencimiento();
    cantidadPresentaciones = r.cantidadPresentaciones();
    contenidoPorPresentacion = r.contenidoPorPresentacion();
    unidadContenido = r.unidadContenido().name();
    precioNetoPorPresentacion = r.precioNetoPorPresentacion();
    impuestoPorcentaje = r.impuestoPorcentaje();
    cantidadUnidadBase = r.cantidadUnidadBase();
    unidadBase = r.unidadBase().name();
    precioNetoUnidadBase = r.precioNetoUnidadBase();
    precioNetoTotal = r.precioNetoTotal();
    valorImpuesto = r.valorImpuesto();
    precioTotal = r.precioTotal();
  }
}
