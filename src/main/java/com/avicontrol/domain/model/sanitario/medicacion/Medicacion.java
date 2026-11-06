package com.avicontrol.domain.model.sanitario.medicacion;

import com.avicontrol.domain.exception.sanitario.medicacion.DatosMedicacionInvalidosException;
import java.time.Instant;
import java.util.UUID;

public record Medicacion(
    UUID id,
    UUID enfermedadId,
    UUID medicamentoId,
    String dosis,
    int duracionDias,
    String descripcion,
    long version,
    Instant fechaCreacion,
    Instant fechaActualizacion) {
  public Medicacion {
    if (id == null
        || enfermedadId == null
        || medicamentoId == null
        || fechaCreacion == null
        || fechaActualizacion == null)
      throw new DatosMedicacionInvalidosException("Identificadores y fechas son obligatorios");
    if (duracionDias <= 0)
      throw new DatosMedicacionInvalidosException("La duración debe ser positiva");
    if (version < 0) throw new DatosMedicacionInvalidosException("Versión inválida");
    dosis = texto(dosis, "dosis");
    descripcion = texto(descripcion, "descripcion");
  }

  public Medicacion actualizar(
      UUID enfermedadId,
      UUID medicamentoId,
      String dosis,
      int duracionDias,
      String descripcion,
      Instant fecha) {
    return new Medicacion(
        id,
        enfermedadId,
        medicamentoId,
        dosis,
        duracionDias,
        descripcion,
        version + 1,
        fechaCreacion,
        fecha);
  }

  private static String texto(String value, String campo) {
    if (value == null || value.isBlank())
      throw new DatosMedicacionInvalidosException(campo + " es obligatorio");
    return value.strip();
  }
}
