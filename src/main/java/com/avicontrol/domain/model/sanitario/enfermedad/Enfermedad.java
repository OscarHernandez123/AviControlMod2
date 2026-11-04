package com.avicontrol.domain.model.sanitario.enfermedad;

import java.util.Objects;
import java.util.UUID;

public final class Enfermedad {

  private final UUID id;
  private String nombre;
  private NivelRiesgo nivelRiesgo;
  private TipoEnfermedad tipo;
  private String descripcionClinica;
  private boolean requiereSacrificioSanitario;

  public Enfermedad(
      UUID id,
      String nombre,
      NivelRiesgo nivelRiesgo,
      TipoEnfermedad tipo,
      String descripcionClinica,
      boolean requiereSacrificioSanitario) {
    this.id = Objects.requireNonNull(id, "El id de la enfermedad es obligatorio");
    actualizar(nombre, nivelRiesgo, tipo, descripcionClinica, requiereSacrificioSanitario);
  }

  public void actualizar(
      String nombre,
      NivelRiesgo nivelRiesgo,
      TipoEnfermedad tipo,
      String descripcionClinica,
      boolean requiereSacrificioSanitario) {
    this.nombre = obligatorio(nombre, "El nombre de la enfermedad es obligatorio");
    this.nivelRiesgo = Objects.requireNonNull(nivelRiesgo, "El nivel de riesgo es obligatorio");
    this.tipo = Objects.requireNonNull(tipo, "El tipo de enfermedad es obligatorio");
    this.descripcionClinica =
        obligatorio(descripcionClinica, "La descripción clínica es obligatoria");
    this.requiereSacrificioSanitario = requiereSacrificioSanitario;
  }

  public UUID id() {
    return id;
  }

  public String nombre() {
    return nombre;
  }

  public NivelRiesgo nivelRiesgo() {
    return nivelRiesgo;
  }

  public TipoEnfermedad tipo() {
    return tipo;
  }

  public String descripcionClinica() {
    return descripcionClinica;
  }

  public boolean requiereSacrificioSanitario() {
    return requiereSacrificioSanitario;
  }

  private String obligatorio(String value, String message) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(message);
    }
    return value.trim();
  }
}
