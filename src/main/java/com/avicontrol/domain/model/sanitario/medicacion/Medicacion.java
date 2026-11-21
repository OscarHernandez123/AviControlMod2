package com.avicontrol.domain.model.sanitario.medicacion;

import com.avicontrol.domain.exception.sanitario.medicacion.DatosMedicacionInvalidosException;
import java.time.Instant;
import java.util.List;
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
    Instant fechaActualizacion,
    String nombreMedicamento,
    String principioActivo,
    List<Integer> diasAplicacion) {
  public Medicacion(
      UUID id,
      UUID enfermedadId,
      UUID medicamentoId,
      String dosis,
      int duracionDias,
      String descripcion,
      long version,
      Instant fechaCreacion,
      Instant fechaActualizacion) {
    this(
        id,
        enfermedadId,
        medicamentoId,
        dosis,
        duracionDias,
        descripcion,
        version,
        fechaCreacion,
        fechaActualizacion,
        null,
        null,
        null);
  }

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
    if ((nombreMedicamento == null) != (principioActivo == null))
      throw new DatosMedicacionInvalidosException(
          "Nombre y principio activo deben informarse juntos");
    if (nombreMedicamento != null) {
      nombreMedicamento = texto(nombreMedicamento, "nombreMedicamento");
      principioActivo = texto(principioActivo, "principioActivo");
    }
    if (diasAplicacion != null) {
      if (diasAplicacion.isEmpty()
          || diasAplicacion.stream().anyMatch(dia -> dia == null || dia < 1 || dia > duracionDias)
          || diasAplicacion.stream().distinct().count() != diasAplicacion.size())
        throw new DatosMedicacionInvalidosException("Días de aplicación inválidos");
      diasAplicacion = List.copyOf(diasAplicacion);
    }
  }

  public Medicacion actualizar(
      UUID enfermedadId,
      UUID medicamentoId,
      String dosis,
      int duracionDias,
      String descripcion,
      Instant fecha) {
    return actualizar(
        enfermedadId,
        medicamentoId,
        dosis,
        duracionDias,
        descripcion,
        nombreMedicamento,
        principioActivo,
        diasAplicacion,
        fecha);
  }

  public Medicacion actualizar(
      UUID enfermedadId,
      UUID medicamentoId,
      String dosis,
      int duracionDias,
      String descripcion,
      String nombreMedicamento,
      String principioActivo,
      List<Integer> diasAplicacion,
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
        fecha,
        nombreMedicamento,
        principioActivo,
        diasAplicacion);
  }

  public boolean aplicaDia(int dia) {
    return dia >= 1
        && dia <= duracionDias
        && (diasAplicacion == null || diasAplicacion.contains(dia));
  }

  private static String texto(String value, String campo) {
    if (value == null || value.isBlank())
      throw new DatosMedicacionInvalidosException(campo + " es obligatorio");
    return value.strip();
  }
}
