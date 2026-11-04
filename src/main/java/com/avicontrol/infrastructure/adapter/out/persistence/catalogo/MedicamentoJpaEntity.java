package com.avicontrol.infrastructure.adapter.out.persistence.catalogo;

import com.avicontrol.domain.model.catalogo.*;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "medicamento")
class MedicamentoJpaEntity {
  @Id UUID id;

  @Column(name = "nombre_comercial", nullable = false)
  String nombreComercial;

  @Column(name = "presentacion_compatible", nullable = false)
  String presentacionCompatible;

  @Enumerated(EnumType.STRING)
  @Column(name = "unidad_base", nullable = false)
  UnidadMedidaMedicamento unidadBase;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  EstadoProductoCatalogo estado;

  protected MedicamentoJpaEntity() {}

  MedicamentoJpaEntity(Medicamento m) {
    id = m.id();
    nombreComercial = m.nombreComercial();
    presentacionCompatible = m.presentacionCompatible();
    unidadBase = m.unidadBase();
    estado = m.estado();
  }
}
