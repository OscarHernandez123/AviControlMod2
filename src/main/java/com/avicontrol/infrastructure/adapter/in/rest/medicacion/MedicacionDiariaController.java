package com.avicontrol.infrastructure.adapter.in.rest.medicacion;

import com.avicontrol.application.sanitario.medicacion.ConsultarMedicacionDiariaGalponUseCase;
import com.avicontrol.application.sanitario.medicacion.ConsultarResumenMedicacionDiariaUseCase;
import com.avicontrol.infrastructure.adapter.in.rest.medicacion.dto.MedicacionDiariaGalponResponse;
import com.avicontrol.infrastructure.adapter.in.rest.medicacion.dto.ResumenMedicacionDiariaResponse;
import com.avicontrol.infrastructure.adapter.in.rest.medicacion.mapper.MedicacionDiariaRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = "basicAuth")
public class MedicacionDiariaController {
  private final ConsultarMedicacionDiariaGalponUseCase detalle;
  private final ConsultarResumenMedicacionDiariaUseCase resumen;
  private final MedicacionDiariaRestMapper mapper;

  public MedicacionDiariaController(
      ConsultarMedicacionDiariaGalponUseCase detalle,
      ConsultarResumenMedicacionDiariaUseCase resumen,
      MedicacionDiariaRestMapper mapper) {
    this.detalle = detalle;
    this.resumen = resumen;
    this.mapper = mapper;
  }

  @GetMapping("/api/galpones/{galponId}/medicacion-diaria")
  @PreAuthorize("hasAnyRole('TRABAJADOR', 'ADMINISTRADOR')")
  @Operation(summary = "Consultar medicación diaria de un galpón")
  public MedicacionDiariaGalponResponse detalle(
      @PathVariable UUID galponId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate fecha) {
    return mapper.response(detalle.consultar(galponId, fecha));
  }

  @GetMapping("/api/medicacion-diaria/resumen")
  @PreAuthorize("hasAnyRole('TRABAJADOR', 'ADMINISTRADOR')")
  @Operation(summary = "Consultar medicación diaria de galpones seleccionados")
  public ResumenMedicacionDiariaResponse resumen(
      @RequestParam(required = false) List<UUID> galponIds,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate fecha) {
    return mapper.response(resumen.consultar(galponIds == null ? List.of() : galponIds, fecha));
  }
}
