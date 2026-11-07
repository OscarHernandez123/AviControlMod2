package com.avicontrol.application.sanitario.medicacion;

import java.util.List;
import java.util.UUID;

public record RegistrarMedicacionCommand(
    UUID enfermedadId,
    UUID medicamentoId,
    String dosis,
    int duracionDias,
    String descripcion,
    String nombreMedicamento,
    String principioActivo,
    List<Integer> diasAplicacion) {
  public RegistrarMedicacionCommand(
      UUID enfermedadId, UUID medicamentoId, String dosis, int duracionDias, String descripcion) {
    this(enfermedadId, medicamentoId, dosis, duracionDias, descripcion, null, null, null);
  }
}
