package com.avicontrol.infrastructure.adapter.in.rest.medicacion.mapper;

import com.avicontrol.application.sanitario.medicacion.EditarMedicacionCommand;
import com.avicontrol.application.sanitario.medicacion.RegistrarMedicacionCommand;
import com.avicontrol.application.sanitario.medicacion.result.MedicacionResult;
import com.avicontrol.infrastructure.adapter.in.rest.medicacion.dto.EditarMedicacionRequest;
import com.avicontrol.infrastructure.adapter.in.rest.medicacion.dto.MedicacionResponse;
import com.avicontrol.infrastructure.adapter.in.rest.medicacion.dto.RegistrarMedicacionRequest;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class MedicacionRestMapper {
  public RegistrarMedicacionCommand command(RegistrarMedicacionRequest r) {
    return new RegistrarMedicacionCommand(
        r.enfermedadId(),
        r.medicamentoId(),
        r.dosis(),
        r.duracionDias(),
        r.descripcion(),
        r.nombreMedicamento(),
        r.principioActivo(),
        r.diasAplicacion());
  }

  public EditarMedicacionCommand command(UUID id, EditarMedicacionRequest r) {
    return new EditarMedicacionCommand(
        id,
        r.enfermedadId(),
        r.medicamentoId(),
        r.dosis(),
        r.duracionDias(),
        r.descripcion(),
        r.nombreMedicamento(),
        r.principioActivo(),
        r.diasAplicacion());
  }

  public MedicacionResponse response(MedicacionResult r) {
    return new MedicacionResponse(
        r.id(),
        r.enfermedadId(),
        r.medicamentoId(),
        r.dosis(),
        r.duracionDias(),
        r.descripcion(),
        r.nombreMedicamento(),
        r.principioActivo(),
        r.diasAplicacion());
  }
}
