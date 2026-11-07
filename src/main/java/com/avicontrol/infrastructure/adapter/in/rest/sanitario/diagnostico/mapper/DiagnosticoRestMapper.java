package com.avicontrol.infrastructure.adapter.in.rest.sanitario.diagnostico.mapper;

import com.avicontrol.application.sanitario.diagnostico.RegistrarDiagnosticoCommand;
import com.avicontrol.application.sanitario.diagnostico.result.DiagnosticoGalponResult;
import com.avicontrol.infrastructure.adapter.in.rest.sanitario.diagnostico.dto.DiagnosticoGalponResponse;
import com.avicontrol.infrastructure.adapter.in.rest.sanitario.diagnostico.dto.RegistrarDiagnosticoRequest;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DiagnosticoRestMapper {
  public RegistrarDiagnosticoCommand command(UUID galponId, RegistrarDiagnosticoRequest request) {
    return new RegistrarDiagnosticoCommand(
        galponId, request.enfermedadId(), request.medicacionId(), request.observaciones());
  }

  public DiagnosticoGalponResponse response(DiagnosticoGalponResult r) {
    return new DiagnosticoGalponResponse(
        r.id(),
        r.galponId(),
        r.loteId(),
        r.enfermedadId(),
        r.enfermedadNombre(),
        r.requiereSacrificioSanitario(),
        r.medicacionId(),
        r.medicamentoId(),
        r.dosis(),
        r.duracionDias(),
        r.descripcionMedicacion(),
        r.observaciones(),
        r.fechaDiagnostico(),
        r.fechaReintegro(),
        r.estado());
  }
}
