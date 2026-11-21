package com.avicontrol.infrastructure.adapter.in.rest.alerta;

import com.avicontrol.application.alerta.ConsultarAlertasSanitariasUseCase;
import com.avicontrol.infrastructure.adapter.in.rest.alerta.dto.AlertaSanitariaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@SecurityRequirement(name = "basicAuth")
public class AlertaSanitariaController {
  private final ConsultarAlertasSanitariasUseCase caso;

  public AlertaSanitariaController(ConsultarAlertasSanitariasUseCase caso) {
    this.caso = caso;
  }

  @GetMapping("/api/alertas-sanitarias")
  @PreAuthorize("hasAnyRole('VETERINARIO','ADMINISTRADOR')")
  @Operation(summary = "Listar alertas sanitarias pendientes")
  public PaginaAlertasResponse listar(
      @RequestParam(defaultValue = "PENDIENTE_ATENCION_VETERINARIA") String estado,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    var pagina = caso.listar(estado, page, size);
    return new PaginaAlertasResponse(
        pagina.elementos().stream().map(AlertaSanitariaResponse::de).toList(),
        pagina.totalElementos(),
        pagina.page(),
        pagina.size());
  }

  public record PaginaAlertasResponse(
      List<AlertaSanitariaResponse> elementos, long totalElementos, int page, int size) {}

  @GetMapping("/api/alertas-sanitarias/{alertaId}")
  @PreAuthorize("hasAnyRole('VETERINARIO','ADMINISTRADOR')")
  @Operation(summary = "Consultar detalle y contexto de alerta sanitaria")
  public AlertaSanitariaResponse detalle(@PathVariable UUID alertaId) {
    return AlertaSanitariaResponse.de(caso.detalle(alertaId));
  }
}
