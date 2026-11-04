package com.avicontrol.infrastructure.adapter.out.persistence.sanitario.enfermedad;

import com.avicontrol.domain.model.sanitario.enfermedad.NivelRiesgo;
import com.avicontrol.domain.model.sanitario.enfermedad.TipoEnfermedad;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;

@Entity
@Table(name = "enfermedad")
class EnfermedadJpaEntity {

  @Id private UUID id;

  @Column(nullable = false, length = 120)
  private String nombre;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private NivelRiesgo nivelRiesgo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TipoEnfermedad tipo;

  @Column(nullable = false, length = 2000)
  private String descripcionClinica;

  @Column(nullable = false)
  private boolean requiereSacrificioSanitario;

  @Version private long version;

  protected EnfermedadJpaEntity() {}

  EnfermedadJpaEntity(
      UUID id,
      String nombre,
      NivelRiesgo nivelRiesgo,
      TipoEnfermedad tipo,
      String descripcionClinica,
      boolean requiereSacrificioSanitario) {
    this.id = id;
    this.nombre = nombre;
    this.nivelRiesgo = nivelRiesgo;
    this.tipo = tipo;
    this.descripcionClinica = descripcionClinica;
    this.requiereSacrificioSanitario = requiereSacrificioSanitario;
  }

  UUID getId() {
    return id;
  }

  String getNombre() {
    return nombre;
  }

  NivelRiesgo getNivelRiesgo() {
    return nivelRiesgo;
  }

  TipoEnfermedad getTipo() {
    return tipo;
  }

  String getDescripcionClinica() {
    return descripcionClinica;
  }

  boolean isRequiereSacrificioSanitario() {
    return requiereSacrificioSanitario;
  }
}
