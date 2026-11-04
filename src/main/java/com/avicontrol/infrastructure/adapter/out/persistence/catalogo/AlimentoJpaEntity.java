package com.avicontrol.infrastructure.adapter.out.persistence.catalogo;

import com.avicontrol.domain.model.catalogo.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "alimento")
class AlimentoJpaEntity {
  @Id UUID id;

  @Column(name = "nombre_comercial", nullable = false)
  String nombreComercial;

  @Column(nullable = false)
  String marca;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  TipoAlimento tipo;

  @Column(name = "peso_nominal_bulto_kg", nullable = false, precision = 19, scale = 4)
  BigDecimal pesoNominalBultoKg;

  @Column(name = "duracion_recomendada_etapa_dias")
  Integer duracionRecomendadaEtapaDias;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  EstadoProductoCatalogo estado;

  protected AlimentoJpaEntity() {}

  AlimentoJpaEntity(Alimento a) {
    id = a.id();
    nombreComercial = a.nombreComercial();
    marca = a.marca();
    tipo = a.tipo();
    pesoNominalBultoKg = a.pesoNominalBultoKg();
    duracionRecomendadaEtapaDias = a.duracionRecomendadaEtapaDias();
    estado = a.estado();
  }
}
