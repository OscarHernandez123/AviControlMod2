package com.avicontrol.domain.model.sanitario.medicacion;

import java.util.UUID;

public record MedicacionDiaria(
    UUID diagnosticoId,
    String loteCodigo,
    ProgresoTratamiento progreso,
    String enfermedad,
    String medicamento,
    String principioActivo,
    String dosisHoy,
    String viaAdministracion,
    boolean aplicaHoy) {
  public MedicacionDiaria {
    if (diagnosticoId == null || progreso == null || enfermedad == null || enfermedad.isBlank())
      throw new IllegalArgumentException("Tratamiento diario incompleto");
    if (!aplicaHoy) dosisHoy = "0";
  }
}
