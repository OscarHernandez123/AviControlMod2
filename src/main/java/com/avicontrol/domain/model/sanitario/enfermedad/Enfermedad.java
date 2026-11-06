package com.avicontrol.domain.model.sanitario.enfermedad;

import com.avicontrol.domain.exception.sanitario.enfermedad.DatosEnfermedadInvalidosException;
import java.time.Instant;
import java.util.UUID;

public record Enfermedad(
    UUID id,
    String nombre,
    NivelRiesgo nivelRiesgo,
    TipoEnfermedad tipo,
    String descripcionClinica,
    Boolean requiereSacrificioSanitario,
    long version,
    Instant fechaCreacion,
    Instant fechaActualizacion) {

  public Enfermedad {
    if (id == null || fechaCreacion == null || fechaActualizacion == null)
      throw new DatosEnfermedadInvalidosException("Identificador y fechas son obligatorios");
    nombre = texto(nombre, "nombre");
    descripcionClinica = texto(descripcionClinica, "descripcionClinica");
    if (nivelRiesgo == null || tipo == null)
      throw new DatosEnfermedadInvalidosException("Riesgo y tipo son obligatorios");
    if (requiereSacrificioSanitario == null)
      throw new DatosEnfermedadInvalidosException(
          "La decisión de sacrificio sanitario debe ser explícita");
    if (version < 0) throw new DatosEnfermedadInvalidosException("Versión inválida");
  }

  public Enfermedad actualizar(
      String nombre,
      NivelRiesgo riesgo,
      TipoEnfermedad tipo,
      String descripcion,
      Boolean requiereSacrificio,
      Instant fecha) {
    return new Enfermedad(
        id,
        nombre,
        riesgo,
        tipo,
        descripcion,
        requiereSacrificio,
        version + 1,
        fechaCreacion,
        fecha);
  }

  private static String texto(String value, String campo) {
    if (value == null || value.isBlank())
      throw new DatosEnfermedadInvalidosException(campo + " es obligatorio");
    return value.strip();
  }
}
