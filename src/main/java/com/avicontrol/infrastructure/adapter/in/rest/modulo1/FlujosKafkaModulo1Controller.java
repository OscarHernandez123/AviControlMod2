package com.avicontrol.infrastructure.adapter.in.rest.modulo1;

import com.avicontrol.application.modulo1.eventos.FlujosKafkaModulo1UseCase;
import com.avicontrol.application.mortalidad.RegistrarMortalidadCommand;
import com.avicontrol.application.sanitario.aislamiento.ValidarAislamientoCommand;
import com.avicontrol.infrastructure.adapter.in.rest.sanitario.aislamiento.dto.ValidarAislamientoRequest;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("modulo1-rest & !local & !test")
public class FlujosKafkaModulo1Controller {
  private final FlujosKafkaModulo1UseCase caso;

  public FlujosKafkaModulo1Controller(FlujosKafkaModulo1UseCase caso) {
    this.caso = caso;
  }

  public record MortalidadRequest(
      @jakarta.validation.constraints.NotNull java.time.OffsetDateTime fechaHoraEvento,
      Object cantidad,
      @jakarta.validation.constraints.NotNull
          com.avicontrol.domain.model.mortalidad.CausaMortalidad causa,
      String observaciones) {}

  @PostMapping("/api/galpones/{galponId}/mortalidad")
  @PreAuthorize("hasAnyRole('TRABAJADOR','ADMINISTRADOR')")
  public ResponseEntity<FlujosKafkaModulo1UseCase.Resultado> mortalidad(
      @PathVariable UUID galponId,
      @Valid @RequestBody MortalidadRequest request,
      @RequestHeader("Idempotency-Key") UUID key,
      Authentication actor) {
    if (!(request.cantidad() instanceof Integer))
      throw new IllegalArgumentException("cantidad debe ser un entero JSON");
    return ResponseEntity.accepted()
        .body(
            caso.mortalidad(
                new RegistrarMortalidadCommand(
                    galponId,
                    request.fechaHoraEvento().toInstant(),
                    (Integer) request.cantidad(),
                    request.causa(),
                    request.observaciones(),
                    actor.getName(),
                    key)));
  }

  @PostMapping("/api/solicitudes-aislamiento/{solicitudId}/validacion")
  @PreAuthorize("hasRole('VETERINARIO')")
  public ResponseEntity<FlujosKafkaModulo1UseCase.Resultado> aislamiento(
      @PathVariable UUID solicitudId,
      @Valid @RequestBody ValidarAislamientoRequest request,
      Authentication actor) {
    var result =
        caso.aislamiento(
            new ValidarAislamientoCommand(
                solicitudId, request.decision(), request.observaciones(), actor.getName()));
    return result.integracion() == null
        ? ResponseEntity.ok(result)
        : ResponseEntity.accepted().body(result);
  }

  @GetMapping("/api/integracion/modulo1/operaciones")
  @PreAuthorize("hasAnyRole('TRABAJADOR','VETERINARIO','ADMINISTRADOR')")
  public java.util.List<FlujosKafkaModulo1UseCase.Resultado> listar() {
    return caso.listar();
  }

  @GetMapping("/api/integracion/modulo1/operaciones/{id}")
  @PreAuthorize("hasAnyRole('TRABAJADOR','VETERINARIO','ADMINISTRADOR')")
  public FlujosKafkaModulo1UseCase.Resultado consultar(@PathVariable UUID id) {
    return caso.consultar(id);
  }
}
