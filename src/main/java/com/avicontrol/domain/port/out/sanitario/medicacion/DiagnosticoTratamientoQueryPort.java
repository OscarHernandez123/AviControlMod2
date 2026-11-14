package com.avicontrol.domain.port.out.sanitario.medicacion;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DiagnosticoTratamientoQueryPort {
  List<Tratamiento> consultar(UUID loteId, LocalDate fecha);

  record Tratamiento(
      UUID diagnosticoId,
      LocalDate fechaInicio,
      int duracionDias,
      String enfermedad,
      String medicamento,
      String principioActivo,
      String dosis,
      String viaAdministracion,
      boolean aplicaHoy) {}
}
