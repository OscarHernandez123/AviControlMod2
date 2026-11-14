package com.avicontrol.infrastructure.adapter.out.persistence.recepcion;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auditoria_recepciones")
public class AuditoriaRecepcionEntity {
  @Id private UUID id;
  private UUID recepcionId;
  private String tipoRecepcion;
  private String accion;
  private String actor;
  private Instant fecha;
  private String valoresAnteriores;
  private String valoresNuevos;

  protected AuditoriaRecepcionEntity() {}

  public AuditoriaRecepcionEntity(
      UUID recepcionId,
      String tipoRecepcion,
      String actor,
      Instant fecha,
      String valoresAnteriores,
      String valoresNuevos) {
    id = UUID.randomUUID();
    this.recepcionId = recepcionId;
    this.tipoRecepcion = tipoRecepcion;
    accion = "EDICION";
    this.actor = actor;
    this.fecha = fecha;
    this.valoresAnteriores = valoresAnteriores;
    this.valoresNuevos = valoresNuevos;
  }
}
