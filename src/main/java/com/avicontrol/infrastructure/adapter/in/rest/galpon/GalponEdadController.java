package com.avicontrol.infrastructure.adapter.in.rest.galpon;

import com.avicontrol.application.galpon.ConsultarEdadPorGalponUseCase;
import com.avicontrol.infrastructure.adapter.in.rest.galpon.dto.EdadLoteResponse;
import com.avicontrol.infrastructure.adapter.in.rest.galpon.mapper.GalponRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = "basicAuth")
public class GalponEdadController {
  private final ConsultarEdadPorGalponUseCase consulta;
  private final GalponRestMapper mapper;

  public GalponEdadController(ConsultarEdadPorGalponUseCase consulta, GalponRestMapper mapper) {
    this.consulta = consulta;
    this.mapper = mapper;
  }

  @GetMapping("/api/galpones/{galponId}/edad-lote")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  @Operation(summary = "Consultar edad inclusiva del lote actual de un galpón")
  public EdadLoteResponse consultar(@PathVariable UUID galponId) {
    return mapper.response(consulta.consultar(galponId));
  }
}
