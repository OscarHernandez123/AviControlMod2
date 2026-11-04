package com.avicontrol.infrastructure.adapter.in.rest.sanitario;

import com.avicontrol.application.sanitario.enfermedad.EditarEnfermedadUseCase;
import com.avicontrol.application.sanitario.enfermedad.EnfermedadCommand;
import com.avicontrol.application.sanitario.enfermedad.EnfermedadResult;
import com.avicontrol.application.sanitario.enfermedad.RegistrarEnfermedadUseCase;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/enfermedades")
public class EnfermedadController {
  private final RegistrarEnfermedadUseCase registrar;
  private final EditarEnfermedadUseCase editar;

  public EnfermedadController(
      RegistrarEnfermedadUseCase registrar, EditarEnfermedadUseCase editar) {
    this.registrar = registrar;
    this.editar = editar;
  }

  @PostMapping
  public ResponseEntity<EnfermedadResponse> registrar(
      @Valid @RequestBody EnfermedadRequest request) {
    EnfermedadResult result = registrar.execute(toCommand(request));
    return ResponseEntity.created(URI.create("/api/enfermedades/" + result.id()))
        .body(toResponse(result));
  }

  @PutMapping("/{enfermedadId}")
  public ResponseEntity<EnfermedadResponse> editar(
      @PathVariable UUID enfermedadId, @Valid @RequestBody EnfermedadRequest request) {
    return ResponseEntity.ok(toResponse(editar.execute(enfermedadId, toCommand(request))));
  }

  private EnfermedadCommand toCommand(EnfermedadRequest request) {
    return new EnfermedadCommand(
        request.nombre(),
        request.nivelRiesgo(),
        request.tipo(),
        request.descripcionClinica(),
        request.requiereSacrificioSanitario());
  }

  private EnfermedadResponse toResponse(EnfermedadResult result) {
    return new EnfermedadResponse(
        result.id(),
        result.nombre(),
        result.nivelRiesgo(),
        result.tipo(),
        result.descripcionClinica(),
        result.requiereSacrificioSanitario());
  }
}
