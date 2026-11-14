package com.avicontrol.infrastructure.adapter.in.rest.mortalidad;

import com.avicontrol.application.mortalidad.RegistrarMortalidadCommand;
import com.avicontrol.application.mortalidad.RegistrarMortalidadUseCase;
import com.avicontrol.infrastructure.adapter.in.rest.mortalidad.dto.RegistrarMortalidadRequest;
import com.avicontrol.infrastructure.adapter.in.rest.mortalidad.dto.RegistroMortalidadResponse;
import com.avicontrol.infrastructure.adapter.in.rest.mortalidad.mapper.MortalidadRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@SecurityRequirement(name = "basicAuth")
public class MortalidadController {
  private final RegistrarMortalidadUseCase caso;
  private final MortalidadRestMapper mapper;

  public MortalidadController(RegistrarMortalidadUseCase caso, MortalidadRestMapper mapper) {
    this.caso = caso;
    this.mapper = mapper;
  }

  @PostMapping("/api/galpones/{galponId}/mortalidad")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('TRABAJADOR','ADMINISTRADOR')")
  @Operation(summary = "Registrar mortalidad y descontar población viva")
  public RegistroMortalidadResponse registrar(
      @PathVariable UUID galponId,
      @Valid @RequestBody RegistrarMortalidadRequest request,
      @RequestHeader(value = "Idempotency-Key", required = false) UUID key,
      Authentication auth) {
    return mapper.response(
        caso.registrar(
            new RegistrarMortalidadCommand(
                galponId,
                request.fechaHoraEvento().toInstant(),
                request.cantidad(),
                request.causa(),
                request.observaciones(),
                auth.getName(),
                key == null ? UUID.randomUUID() : key)));
  }
}
