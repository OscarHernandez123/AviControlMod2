package com.avicontrol.infrastructure.adapter.in.rest.alimentacion;

import com.avicontrol.application.alimentacion.ConsultarAlimentoGalponDiaUseCase;
import com.avicontrol.application.alimentacion.ConsultarResumenAlimentoDiarioUseCase;
import com.avicontrol.application.alimentacion.RegistrarSuministroDiarioUseCase;
import com.avicontrol.infrastructure.adapter.in.rest.alimentacion.dto.AlimentoDiarioGalponResponse;
import com.avicontrol.infrastructure.adapter.in.rest.alimentacion.dto.RegistrarSuministroDiarioRequest;
import com.avicontrol.infrastructure.adapter.in.rest.alimentacion.dto.ResumenAlimentoDiarioResponse;
import com.avicontrol.infrastructure.adapter.in.rest.alimentacion.dto.SuministroDiarioResponse;
import com.avicontrol.infrastructure.adapter.in.rest.alimentacion.mapper.AlimentacionDiariaRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = "basicAuth")
public class AlimentacionDiariaController {
  private final ConsultarAlimentoGalponDiaUseCase detalle;
  private final ConsultarResumenAlimentoDiarioUseCase resumen;
  private final RegistrarSuministroDiarioUseCase registro;
  private final AlimentacionDiariaRestMapper mapper;

  public AlimentacionDiariaController(
      ConsultarAlimentoGalponDiaUseCase detalle,
      ConsultarResumenAlimentoDiarioUseCase resumen,
      RegistrarSuministroDiarioUseCase registro,
      AlimentacionDiariaRestMapper mapper) {
    this.detalle = detalle;
    this.resumen = resumen;
    this.registro = registro;
    this.mapper = mapper;
  }

  @GetMapping("/api/galpones/{galponId}/alimento-diario")
  @PreAuthorize("hasAnyRole('TRABAJADOR', 'ADMINISTRADOR')")
  @Operation(summary = "Consultar alimento diario de un galpón")
  public AlimentoDiarioGalponResponse detalle(
      @PathVariable UUID galponId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate fecha) {
    return mapper.response(detalle.consultar(galponId, fecha));
  }

  @GetMapping("/api/alimento-diario/resumen")
  @PreAuthorize("hasAnyRole('TRABAJADOR', 'ADMINISTRADOR')")
  @Operation(summary = "Consultar alimento de galpones seleccionados")
  public ResumenAlimentoDiarioResponse resumen(
      @RequestParam(required = false) List<UUID> galponIds,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate fecha) {
    return mapper.response(resumen.consultar(galponIds == null ? List.of() : galponIds, fecha));
  }

  @PostMapping("/api/galpones/{galponId}/suministros-alimento")
  @PreAuthorize("hasAnyRole('TRABAJADOR', 'ADMINISTRADOR')")
  @Operation(summary = "Registrar suministro real con descuento FEFO")
  public ResponseEntity<SuministroDiarioResponse> registrar(
      @PathVariable UUID galponId,
      @Valid @RequestBody RegistrarSuministroDiarioRequest request,
      Authentication authentication) {
    var resultado =
        registro.registrar(
            galponId,
            request.cantidadKg(),
            request.fechaAplicacion(),
            request.idempotencyKey(),
            authentication.getName());
    return ResponseEntity.status(resultado.repetido() ? 200 : 201)
        .body(mapper.response(resultado.suministro()));
  }
}
