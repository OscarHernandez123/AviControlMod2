package com.avicontrol.application.sanitario.diagnostico.result;

import com.avicontrol.domain.model.sanitario.diagnostico.DiagnosticoGalpon;
import com.avicontrol.domain.model.sanitario.diagnostico.EstadoDiagnostico;
import java.time.LocalDate;
import java.util.UUID;

public record DiagnosticoGalponResult(
    UUID id,
    UUID galponId,
    UUID loteId,
    UUID enfermedadId,
    String enfermedadNombre,
    boolean requiereSacrificioSanitario,
    UUID medicacionId,
    UUID medicamentoId,
    String dosis,
    Integer duracionDias,
    String descripcionMedicacion,
    String observaciones,
    LocalDate fechaDiagnostico,
    LocalDate fechaReintegro,
    EstadoDiagnostico estado) {
  public static DiagnosticoGalponResult de(DiagnosticoGalpon d) {
    return new DiagnosticoGalponResult(
        d.id(),
        d.galponId(),
        d.loteId(),
        d.enfermedadId(),
        d.enfermedadNombre(),
        d.requiereSacrificioSanitario(),
        d.medicacionId(),
        d.medicamentoId(),
        d.dosis(),
        d.duracionDias(),
        d.descripcionMedicacion(),
        d.observaciones(),
        d.fechaDiagnostico(),
        d.fechaReintegro(),
        d.estado());
  }
}
