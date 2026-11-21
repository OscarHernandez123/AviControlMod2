package com.avicontrol.infrastructure.demo;

import com.avicontrol.application.sanitario.diagnostico.result.DiagnosticoGalponResult;
import com.avicontrol.infrastructure.adapter.in.rest.sanitario.diagnostico.dto.DiagnosticoGalponResponse;
import com.avicontrol.infrastructure.adapter.in.rest.sanitario.diagnostico.mapper.DiagnosticoRestMapper;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("local & !modulo1-rest")
@ConditionalOnProperty(name = "avicontrol.demo.enabled", havingValue = "true")
public class DemoReintegroController {
  private final DemoReintegroService servicio;
  private final DiagnosticoRestMapper mapper;

  public DemoReintegroController(DemoReintegroService servicio, DiagnosticoRestMapper mapper) {
    this.servicio = servicio;
    this.mapper = mapper;
  }

  @PostMapping("/api/demo/diagnosticos/{id}/adelantar-reintegro")
  @PreAuthorize("hasRole('VETERINARIO')")
  public DiagnosticoGalponResponse adelantar(
      @PathVariable UUID id, @RequestBody Solicitud request, Authentication authentication) {
    return mapper.response(
        DiagnosticoGalponResult.de(
            servicio.adelantar(id, request.motivo(), authentication.getName())));
  }

  public record Solicitud(String motivo) {}
}
