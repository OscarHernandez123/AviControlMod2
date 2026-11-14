package com.avicontrol.infrastructure.adapter.in.rest.sanitario.diagnostico;

import com.avicontrol.application.sanitario.diagnostico.RegistrarDiagnosticoUseCase;
import com.avicontrol.application.sanitario.diagnostico.result.DiagnosticoGalponResult;
import com.avicontrol.domain.port.out.sanitario.diagnostico.DiagnosticoGalponRepositoryPort;
import com.avicontrol.infrastructure.adapter.in.rest.sanitario.diagnostico.dto.DiagnosticoGalponResponse;
import com.avicontrol.infrastructure.adapter.in.rest.sanitario.diagnostico.dto.RegistrarDiagnosticoRequest;
import com.avicontrol.infrastructure.adapter.in.rest.sanitario.diagnostico.mapper.DiagnosticoRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "basicAuth")
public class DiagnosticoController {
  private final RegistrarDiagnosticoUseCase registro;
  private final DiagnosticoGalponRepositoryPort diagnosticos;
  private final DiagnosticoRestMapper mapper;

  public DiagnosticoController(
      RegistrarDiagnosticoUseCase registro,
      DiagnosticoGalponRepositoryPort diagnosticos,
      DiagnosticoRestMapper mapper) {
    this.registro = registro;
    this.diagnosticos = diagnosticos;
    this.mapper = mapper;
  }

  @PostMapping("/galpones/{galponId}/diagnosticos")
  @PreAuthorize("hasRole('VETERINARIO')")
  @Operation(
      summary = "Registrar diagnóstico",
      description =
          "Una enfermedad tratable requiere medicación compatible; una enfermedad de sacrificio sanitario no admite medicación")
  public ResponseEntity<DiagnosticoGalponResponse> registrar(
      @PathVariable UUID galponId, @Valid @RequestBody RegistrarDiagnosticoRequest request) {
    DiagnosticoGalponResponse response =
        mapper.response(registro.registrar(mapper.command(galponId, request)));
    return ResponseEntity.created(URI.create("/api/diagnosticos/" + response.id())).body(response);
  }

  @GetMapping("/diagnosticos/{diagnosticoId}")
  @PreAuthorize("hasRole('VETERINARIO')")
  @Operation(summary = "Consultar diagnóstico y su instantánea clínica")
  public DiagnosticoGalponResponse detalle(@PathVariable UUID diagnosticoId) {
    return mapper.response(
        DiagnosticoGalponResult.de(
            diagnosticos
                .buscar(diagnosticoId)
                .orElseThrow(() -> new NoSuchElementException("Diagnóstico no encontrado"))));
  }

  @GetMapping("/diagnosticos")
  @PreAuthorize("hasRole('VETERINARIO')")
  @Operation(summary = "Listar diagnósticos registrados")
  public List<DiagnosticoGalponResponse> listar() {
    return diagnosticos.listar().stream()
        .map(DiagnosticoGalponResult::de)
        .map(mapper::response)
        .toList();
  }
}
