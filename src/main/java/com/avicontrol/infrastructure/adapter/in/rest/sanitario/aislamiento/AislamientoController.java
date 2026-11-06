package com.avicontrol.infrastructure.adapter.in.rest.sanitario.aislamiento;

import com.avicontrol.application.sanitario.aislamiento.ValidarAislamientoUseCase;
import com.avicontrol.infrastructure.adapter.in.rest.sanitario.aislamiento.dto.ValidacionAislamientoResponse;
import com.avicontrol.infrastructure.adapter.in.rest.sanitario.aislamiento.dto.ValidarAislamientoRequest;
import com.avicontrol.infrastructure.adapter.in.rest.sanitario.aislamiento.mapper.AislamientoRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/solicitudes-aislamiento")
@SecurityRequirement(name = "basicAuth")
public class AislamientoController {
  private final ValidarAislamientoUseCase validar;
  private final AislamientoRestMapper mapper;

  public AislamientoController(ValidarAislamientoUseCase validar, AislamientoRestMapper mapper) {
    this.validar = validar;
    this.mapper = mapper;
  }

  @PostMapping("/{solicitudId}/validacion")
  @PreAuthorize("hasRole('VETERINARIO')")
  @Operation(
      summary = "Decidir aislamiento",
      description = "Acepta sólo si el galpón continúa en productiva")
  public ValidacionAislamientoResponse validar(
      @PathVariable UUID solicitudId,
      @Valid @RequestBody ValidarAislamientoRequest request,
      Authentication actor) {
    return mapper.response(validar.validar(mapper.command(solicitudId, request, actor.getName())));
  }
}
