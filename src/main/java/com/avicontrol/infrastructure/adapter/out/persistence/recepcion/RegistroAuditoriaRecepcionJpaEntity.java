package com.avicontrol.infrastructure.adapter.out.persistence.recepcion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auditoria_recepcion")
class RegistroAuditoriaRecepcionJpaEntity {
  @Id UUID id;
  UUID recepcionId;
  String actorId;
  Instant fechaHora;

  @Column(columnDefinition = "TEXT")
  String valoresAnteriores;

  @Column(columnDefinition = "TEXT")
  String valoresNuevos;

  protected RegistroAuditoriaRecepcionJpaEntity() {}

  RegistroAuditoriaRecepcionJpaEntity(
      com.avicontrol.domain.model.recepcion.RegistroAuditoriaRecepcion r) {
    id = r.id();
    recepcionId = r.recepcionId();
    actorId = r.actorId();
    fechaHora = r.fechaHora();
    valoresAnteriores = r.valoresAnteriores();
    valoresNuevos = r.valoresNuevos();
  }
}
