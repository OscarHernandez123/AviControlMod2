package com.avicontrol.infrastructure.adapter.in.rest.mortalidad;

import com.avicontrol.application.mortalidad.RegistrarMortalidadAnormalCommand;
import com.avicontrol.application.mortalidad.RegistrarMortalidadAnormalUseCase;
import com.avicontrol.infrastructure.adapter.in.rest.mortalidad.dto.RegistrarMortalidadAnormalRequest;
import com.avicontrol.infrastructure.adapter.in.rest.mortalidad.dto.RegistroMortalidadAnormalResponse;
import com.avicontrol.infrastructure.adapter.in.rest.mortalidad.mapper.MortalidadAnormalRestMapper;
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
public class MortalidadAnormalController {
  private final RegistrarMortalidadAnormalUseCase caso;
  private final MortalidadAnormalRestMapper mapper;

  public MortalidadAnormalController(
      RegistrarMortalidadAnormalUseCase caso, MortalidadAnormalRestMapper mapper) {
    this.caso = caso;
    this.mapper = mapper;
  }

  @PostMapping("/api/galpones/{galponId}/mortalidad-anormal")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('TRABAJADOR','ADMINISTRADOR')")
  @Operation(summary = "Reportar mortalidad anormal y generar alerta sanitaria")
  public RegistroMortalidadAnormalResponse registrar(
      @PathVariable UUID galponId,
      @Valid @RequestBody RegistrarMortalidadAnormalRequest request,
      @RequestHeader(value = "Idempotency-Key", required = false) UUID key,
      Authentication auth) {
    return mapper.response(
        caso.registrar(
            new RegistrarMortalidadAnormalCommand(
                galponId,
                request.fechaHoraEvento().toInstant(),
                request.cantidad(),
                request.causaProbable(),
                request.observaciones(),
                auth.getName(),
                key)));
  }
}
