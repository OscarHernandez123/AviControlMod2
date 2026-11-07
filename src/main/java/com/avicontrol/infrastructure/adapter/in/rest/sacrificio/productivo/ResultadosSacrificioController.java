package com.avicontrol.infrastructure.adapter.in.rest.sacrificio.productivo;

import com.avicontrol.application.sacrificio.productivo.ConsultarResultadoSacrificioUseCase;
import com.avicontrol.application.sacrificio.productivo.CorregirResultadoSacrificioUseCase;
import com.avicontrol.application.sacrificio.productivo.RegistrarResultadoSacrificioUseCase;
import com.avicontrol.infrastructure.adapter.in.rest.sacrificio.productivo.dto.CorregirResultadoSacrificioRequest;
import com.avicontrol.infrastructure.adapter.in.rest.sacrificio.productivo.dto.RegistrarResultadoSacrificioRequest;
import com.avicontrol.infrastructure.adapter.in.rest.sacrificio.productivo.dto.ResultadoSacrificioResponse;
import com.avicontrol.infrastructure.adapter.in.rest.sacrificio.productivo.mapper.ResultadoSacrificioRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
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
public class ResultadosSacrificioController {
  private final RegistrarResultadoSacrificioUseCase registro;
  private final CorregirResultadoSacrificioUseCase correccion;
  private final ConsultarResultadoSacrificioUseCase consulta;
  private final ResultadoSacrificioRestMapper mapper;

  public ResultadosSacrificioController(
      RegistrarResultadoSacrificioUseCase registro,
      CorregirResultadoSacrificioUseCase correccion,
      ConsultarResultadoSacrificioUseCase consulta,
      ResultadoSacrificioRestMapper mapper) {
    this.registro = registro;
    this.correccion = correccion;
    this.consulta = consulta;
    this.mapper = mapper;
  }

  @PostMapping("/api/ordenes-sacrificio/{ordenId}/resultados")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  @Operation(summary = "Registrar resultado final de una orden ejecutada")
  public ResultadoSacrificioResponse registrar(
      @PathVariable UUID ordenId, @Valid @RequestBody RegistrarResultadoSacrificioRequest request) {
    return mapper.response(
        registro.registrar(ordenId, request.cantidadFinalPollos(), request.pesoTotalKg()));
  }

  @PutMapping("/api/ordenes-sacrificio/{ordenId}/resultados")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  @Operation(summary = "Corregir resultado disponible antes de su utilización")
  public ResultadoSacrificioResponse corregir(
      @PathVariable UUID ordenId, @Valid @RequestBody CorregirResultadoSacrificioRequest request) {
    return mapper.response(
        correccion.corregir(ordenId, request.cantidadFinalPollos(), request.pesoTotalKg()));
  }

  @GetMapping("/api/ordenes-sacrificio/{ordenId}/resultados")
  @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'MODULO3')")
  @Operation(summary = "Consultar resultado final para revisión o liquidación")
  public ResultadoSacrificioResponse consultar(@PathVariable UUID ordenId) {
    return mapper.response(consulta.consultar(ordenId));
  }
}
