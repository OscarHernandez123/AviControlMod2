package com.avicontrol.infrastructure.adapter.out.persistence.recepcion;

import com.avicontrol.domain.model.catalogo.EtapaCrianza;
import com.avicontrol.domain.model.recepcion.EstadoRecepcion;
import com.avicontrol.domain.model.recepcion.RecepcionAlimento;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "recepciones_alimento")
public class RecepcionAlimentoEntity {
  @Id private UUID id;
  private UUID alimentoId;
  private String nombreAlimento;
  private String tipoAlimento;
  private String codigoLote;
  private LocalDate fechaIngreso;
  private LocalDate fechaVencimiento;
  private int cantidadBultos;
  private BigDecimal pesoPorBultoKg;
  private BigDecimal precioNetoPorBulto;
  private BigDecimal impuestoPorcentaje;
  private BigDecimal cantidadTotalKg;
  private BigDecimal precioNetoKg;
  private BigDecimal precioNetoTotal;
  private BigDecimal valorImpuesto;
  private BigDecimal precioTotal;
  private String estado;
  private String actor;

  protected RecepcionAlimentoEntity() {}

  public RecepcionAlimentoEntity(RecepcionAlimento r) {
    id = r.id();
    alimentoId = r.alimentoId();
    nombreAlimento = r.nombreAlimento();
    tipoAlimento = r.tipoAlimento().name();
    codigoLote = r.codigoLote();
    fechaIngreso = r.fechaIngreso();
    fechaVencimiento = r.fechaVencimiento();
    cantidadBultos = r.cantidadBultos();
    pesoPorBultoKg = r.pesoPorBultoKg();
    precioNetoPorBulto = r.precioNetoPorBulto();
    impuestoPorcentaje = r.impuestoPorcentaje();
    cantidadTotalKg = r.cantidadTotalKg();
    precioNetoKg = r.precioNetoKg();
    precioNetoTotal = r.precioNetoTotal();
    valorImpuesto = r.valorImpuesto();
    precioTotal = r.precioTotal();
    estado = r.estado().name();
    actor = r.actor();
  }

  public RecepcionAlimento toDomain() {
    return new RecepcionAlimento(
        id,
        alimentoId,
        nombreAlimento,
        tipoAlimento == null ? null : EtapaCrianza.valueOf(tipoAlimento),
        codigoLote,
        fechaIngreso,
        fechaVencimiento,
        cantidadBultos,
        pesoPorBultoKg,
        precioNetoPorBulto,
        impuestoPorcentaje,
        cantidadTotalKg,
        precioNetoKg,
        precioNetoTotal,
        valorImpuesto,
        precioTotal,
        EstadoRecepcion.valueOf(estado),
        actor);
  }

  public void actualizar(RecepcionAlimento r) {
    alimentoId = r.alimentoId();
    nombreAlimento = r.nombreAlimento();
    fechaVencimiento = r.fechaVencimiento();
    cantidadBultos = r.cantidadBultos();
    pesoPorBultoKg = r.pesoPorBultoKg();
    precioNetoPorBulto = r.precioNetoPorBulto();
    impuestoPorcentaje = r.impuestoPorcentaje();
    cantidadTotalKg = r.cantidadTotalKg();
    precioNetoKg = r.precioNetoKg();
    precioNetoTotal = r.precioNetoTotal();
    valorImpuesto = r.valorImpuesto();
    precioTotal = r.precioTotal();
  }
}
