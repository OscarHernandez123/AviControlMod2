package com.avicontrol.application.sanitario.medicacion.result;

import com.avicontrol.domain.model.sanitario.medicacion.Medicacion;
import java.util.List;
import java.util.UUID;

public record MedicacionResult(
    UUID id,
    UUID enfermedadId,
    UUID medicamentoId,
    String dosis,
    int duracionDias,
    String descripcion,
    String nombreMedicamento,
    String principioActivo,
    List<Integer> diasAplicacion) {
  public static MedicacionResult de(Medicacion m) {
    return new MedicacionResult(
        m.id(),
        m.enfermedadId(),
        m.medicamentoId(),
        m.dosis(),
        m.duracionDias(),
        m.descripcion(),
        m.nombreMedicamento(),
        m.principioActivo(),
        m.diasAplicacion());
  }
}
