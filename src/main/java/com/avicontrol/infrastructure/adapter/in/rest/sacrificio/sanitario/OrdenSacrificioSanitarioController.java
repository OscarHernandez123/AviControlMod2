package com.avicontrol.infrastructure.adapter.in.rest.sacrificio.sanitario;

import com.avicontrol.application.sacrificio.sanitario.ConfirmarEjecucionSacrificioSanitarioUseCase;
import com.avicontrol.application.sacrificio.sanitario.EmitirOrdenSacrificioSanitarioUseCase;
import com.avicontrol.infrastructure.adapter.in.rest.sacrificio.sanitario.dto.OrdenSacrificioSanitarioResponse;
import com.avicontrol.infrastructure.adapter.in.rest.sacrificio.sanitario.mapper.OrdenSacrificioSanitarioRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = "basicAuth")
public class OrdenSacrificioSanitarioController {
  private final EmitirOrdenSacrificioSanitarioUseCase emision;
  private final ConfirmarEjecucionSacrificioSanitarioUseCase ejecucion;
  private final OrdenSacrificioSanitarioRestMapper mapper;

  public OrdenSacrificioSanitarioController(
      EmitirOrdenSacrificioSanitarioUseCase emision,
      ConfirmarEjecucionSacrificioSanitarioUseCase ejecucion,
      OrdenSacrificioSanitarioRestMapper mapper) {
    this.emision = emision;
    this.ejecucion = ejecucion;
    this.mapper = mapper;
  }

  @PostMapping("/api/galpones/{galponId}/ordenes-sacrificio-sanitario")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('VETERINARIO')")
  @Operation(
      summary = "Emitir orden de sacrificio sanitario total",
      description = "Solo veterinario; deja la orden PENDIENTE sin modificar el lote")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Orden emitida"),
    @ApiResponse(responseCode = "400", description = "Identificador inválido"),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Rol insuficiente"),
    @ApiResponse(responseCode = "404", description = "Diagnóstico ausente"),
    @ApiResponse(responseCode = "409", description = "Diagnóstico, estado o población inválidos"),
    @ApiResponse(responseCode = "503", description = "Módulo 1 no disponible")
  })
  public OrdenSacrificioSanitarioResponse emitir(@PathVariable UUID galponId) {
    return mapper.response(emision.emitir(galponId));
  }

  @PostMapping("/api/ordenes-sacrificio-sanitario/{ordenId}/ejecucion")
  @PreAuthorize("hasRole('VETERINARIO')")
  @Operation(
      summary = "Confirmar sacrificio sanitario total",
      description = "Usa la población vigente y ejecuta una sola vez")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Orden ejecutada"),
    @ApiResponse(responseCode = "400", description = "Identificador inválido"),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Rol insuficiente"),
    @ApiResponse(responseCode = "404", description = "Orden ausente"),
    @ApiResponse(responseCode = "409", description = "Orden ejecutada o precondición inválida"),
    @ApiResponse(responseCode = "503", description = "Módulo 1 no disponible")
  })
  public OrdenSacrificioSanitarioResponse confirmar(@PathVariable UUID ordenId) {
    return mapper.response(ejecucion.confirmar(ordenId));
  }
}
