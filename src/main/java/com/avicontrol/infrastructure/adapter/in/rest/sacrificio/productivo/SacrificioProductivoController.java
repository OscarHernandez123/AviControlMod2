package com.avicontrol.infrastructure.adapter.in.rest.sacrificio.productivo;

import com.avicontrol.application.sacrificio.productivo.CancelarOrdenSacrificioUseCase;
import com.avicontrol.application.sacrificio.productivo.ConsultarOrdenesSacrificioUseCase;
import com.avicontrol.application.sacrificio.productivo.ConsultarResumenOrdenesSacrificioUseCase;
import com.avicontrol.application.sacrificio.productivo.ProgramarOrdenSacrificioUseCase;
import com.avicontrol.application.sacrificio.productivo.ReprogramarOrdenSacrificioUseCase;
import com.avicontrol.infrastructure.adapter.in.rest.sacrificio.productivo.dto.OrdenSacrificioResponse;
import com.avicontrol.infrastructure.adapter.in.rest.sacrificio.productivo.dto.ProgramarOrdenSacrificioRequest;
import com.avicontrol.infrastructure.adapter.in.rest.sacrificio.productivo.dto.ReprogramarOrdenSacrificioRequest;
import com.avicontrol.infrastructure.adapter.in.rest.sacrificio.productivo.dto.ResumenOrdenesSacrificioResponse;
import com.avicontrol.infrastructure.adapter.in.rest.sacrificio.productivo.mapper.SacrificioProductivoRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = "basicAuth")
public class SacrificioProductivoController {
  private final ProgramarOrdenSacrificioUseCase programacion;
  private final ReprogramarOrdenSacrificioUseCase reprogramacion;
  private final CancelarOrdenSacrificioUseCase cancelacion;
  private final ConsultarResumenOrdenesSacrificioUseCase resumen;
  private final ConsultarOrdenesSacrificioUseCase consulta;
  private final SacrificioProductivoRestMapper mapper;

  public SacrificioProductivoController(
      ProgramarOrdenSacrificioUseCase programacion,
      ReprogramarOrdenSacrificioUseCase reprogramacion,
      CancelarOrdenSacrificioUseCase cancelacion,
      ConsultarResumenOrdenesSacrificioUseCase resumen,
      ConsultarOrdenesSacrificioUseCase consulta,
      SacrificioProductivoRestMapper mapper) {
    this.programacion = programacion;
    this.reprogramacion = reprogramacion;
    this.cancelacion = cancelacion;
    this.resumen = resumen;
    this.consulta = consulta;
    this.mapper = mapper;
  }

  @GetMapping("/api/ordenes-sacrificio")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  @Operation(summary = "Consultar órdenes de sacrificio productivo")
  public List<OrdenSacrificioResponse> listar() {
    return consulta.listar().stream().map(mapper::response).toList();
  }

  @PostMapping("/api/galpones/{galponId}/ordenes-sacrificio")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  @Operation(summary = "Programar sacrificio productivo", description = "Solo administrador")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Orden programada"),
    @ApiResponse(responseCode = "400", description = "Fecha o identificador inválido"),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Rol insuficiente"),
    @ApiResponse(responseCode = "404", description = "Galpón o lote ausente"),
    @ApiResponse(responseCode = "409", description = "Precondición incumplida"),
    @ApiResponse(responseCode = "503", description = "Módulo 1 no disponible")
  })
  public OrdenSacrificioResponse programar(
      @PathVariable UUID galponId, @Valid @RequestBody ProgramarOrdenSacrificioRequest request) {
    return mapper.response(
        programacion.programar(galponId, request.fechaHoraProgramada().toInstant()));
  }

  @PutMapping("/api/ordenes-sacrificio/{ordenId}/reprogramacion")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  @Operation(summary = "Reprogramar orden pendiente", description = "Solo administrador")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Orden reprogramada"),
    @ApiResponse(responseCode = "400", description = "Fecha o identificador inválido"),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Rol insuficiente"),
    @ApiResponse(responseCode = "404", description = "Orden ausente"),
    @ApiResponse(responseCode = "409", description = "Orden no modificable"),
    @ApiResponse(responseCode = "503", description = "Módulo 1 no disponible")
  })
  public OrdenSacrificioResponse reprogramar(
      @PathVariable UUID ordenId, @Valid @RequestBody ReprogramarOrdenSacrificioRequest request) {
    return mapper.response(
        reprogramacion.reprogramar(ordenId, request.fechaHoraProgramada().toInstant()));
  }

  @PostMapping("/api/ordenes-sacrificio/{ordenId}/cancelacion")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  @Operation(summary = "Cancelar orden pendiente", description = "Solo administrador")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Orden cancelada"),
    @ApiResponse(responseCode = "400", description = "Identificador inválido"),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Rol insuficiente"),
    @ApiResponse(responseCode = "404", description = "Orden ausente"),
    @ApiResponse(responseCode = "409", description = "Orden no modificable")
  })
  public OrdenSacrificioResponse cancelar(@PathVariable UUID ordenId) {
    return mapper.response(cancelacion.cancelar(ordenId));
  }

  @GetMapping("/api/ordenes-sacrificio/resumen")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  @Operation(summary = "Consultar resumen de órdenes", description = "Solo administrador")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Total y pendientes"),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Rol insuficiente")
  })
  public ResumenOrdenesSacrificioResponse resumen() {
    return mapper.response(resumen.consultar());
  }
}
