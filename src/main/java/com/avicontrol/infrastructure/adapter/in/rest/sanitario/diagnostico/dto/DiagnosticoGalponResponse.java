package com.avicontrol.infrastructure.adapter.in.rest.sanitario.diagnostico.dto;

import com.avicontrol.domain.model.sanitario.diagnostico.EstadoDiagnostico;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DiagnosticoGalponResponse(
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
    EstadoDiagnostico estado,
    String medicamentoNombre,
    String principioActivo,
    List<Integer> diasAplicacion) {}
