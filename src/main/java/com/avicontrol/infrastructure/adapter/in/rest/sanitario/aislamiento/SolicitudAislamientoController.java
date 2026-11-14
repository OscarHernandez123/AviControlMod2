package com.avicontrol.infrastructure.adapter.in.rest.sanitario.aislamiento;

import com.avicontrol.application.sanitario.aislamiento.CrearSolicitudAislamientoUseCase;
import com.avicontrol.application.sanitario.aislamiento.result.SolicitudAislamientoResult;
import com.avicontrol.domain.exception.sanitario.aislamiento.SolicitudAislamientoNoEncontradaException;
import com.avicontrol.domain.port.out.sanitario.aislamiento.SolicitudAislamientoRepositoryPort;
import com.avicontrol.infrastructure.adapter.in.rest.sanitario.aislamiento.dto.CrearSolicitudAislamientoRequest;
import com.avicontrol.infrastructure.adapter.in.rest.sanitario.aislamiento.dto.SolicitudAislamientoResponse;
import com.avicontrol.infrastructure.adapter.in.rest.sanitario.aislamiento.mapper.SolicitudAislamientoRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "basicAuth")
public class SolicitudAislamientoController {
  private final CrearSolicitudAislamientoUseCase crear;
  private final SolicitudAislamientoRepositoryPort solicitudes;
  private final SolicitudAislamientoRestMapper mapper;

  public SolicitudAislamientoController(
      CrearSolicitudAislamientoUseCase crear,
      SolicitudAislamientoRepositoryPort solicitudes,
      SolicitudAislamientoRestMapper mapper) {
    this.crear = crear;
    this.solicitudes = solicitudes;
    this.mapper = mapper;
  }

  @PostMapping("/galpones/{galponId}/solicitudes-aislamiento")
  @PreAuthorize("hasAnyRole('TRABAJADOR', 'ADMINISTRADOR')")
  @Operation(summary = "Solicitar validación de aislamiento")
  public ResponseEntity<SolicitudAislamientoResponse> registrar(
      @PathVariable UUID galponId,
      @Valid @RequestBody CrearSolicitudAislamientoRequest request,
      Authentication actor) {
    SolicitudAislamientoResponse response =
        mapper.response(crear.crear(mapper.command(galponId, request, actor.getName())));
    return ResponseEntity.created(URI.create("/api/solicitudes-aislamiento/" + response.id()))
        .body(response);
  }

  @GetMapping("/solicitudes-aislamiento/pendientes")
  @PreAuthorize("hasRole('VETERINARIO')")
  @Operation(summary = "Consultar solicitudes pendientes para evaluación veterinaria")
  public List<SolicitudAislamientoResponse> pendientes() {
    return solicitudes.pendientes().stream()
        .map(SolicitudAislamientoResult::de)
        .map(mapper::response)
        .toList();
  }

  @GetMapping("/solicitudes-aislamiento/{solicitudId}")
  @PreAuthorize("hasRole('VETERINARIO')")
  @Operation(summary = "Consultar solicitud de aislamiento")
  public SolicitudAislamientoResponse detalle(@PathVariable UUID solicitudId) {
    return mapper.response(
        SolicitudAislamientoResult.de(
            solicitudes
                .buscar(solicitudId)
                .orElseThrow(SolicitudAislamientoNoEncontradaException::new)));
  }
}
