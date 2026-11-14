package com.avicontrol.infrastructure.adapter.in.rest.sanitario.enfermedad.mapper;

import com.avicontrol.application.sanitario.enfermedad.EditarEnfermedadCommand;
import com.avicontrol.application.sanitario.enfermedad.RegistrarEnfermedadCommand;
import com.avicontrol.application.sanitario.enfermedad.result.EnfermedadResult;
import com.avicontrol.infrastructure.adapter.in.rest.sanitario.enfermedad.dto.EditarEnfermedadRequest;
import com.avicontrol.infrastructure.adapter.in.rest.sanitario.enfermedad.dto.EnfermedadResponse;
import com.avicontrol.infrastructure.adapter.in.rest.sanitario.enfermedad.dto.RegistrarEnfermedadRequest;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class EnfermedadRestMapper {
  public RegistrarEnfermedadCommand command(RegistrarEnfermedadRequest r) {
    return new RegistrarEnfermedadCommand(
        r.nombre(),
        r.nivelRiesgo(),
        r.tipo(),
        r.descripcionClinica(),
        r.requiereSacrificioSanitario());
  }

  public EditarEnfermedadCommand command(UUID id, EditarEnfermedadRequest r) {
    return new EditarEnfermedadCommand(
        id,
        r.nombre(),
        r.nivelRiesgo(),
        r.tipo(),
        r.descripcionClinica(),
        r.requiereSacrificioSanitario());
  }

  public EnfermedadResponse response(EnfermedadResult r) {
    return new EnfermedadResponse(
        r.id(),
        r.nombre(),
        r.nivelRiesgo(),
        r.tipo(),
        r.descripcionClinica(),
        r.requiereSacrificioSanitario());
  }
}
