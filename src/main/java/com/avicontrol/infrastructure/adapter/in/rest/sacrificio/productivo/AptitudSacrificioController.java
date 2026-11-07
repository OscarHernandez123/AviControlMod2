package com.avicontrol.infrastructure.adapter.in.rest.sacrificio.productivo;

import com.avicontrol.application.sacrificio.productivo.ValidarAptitudSacrificioCommand;
import com.avicontrol.application.sacrificio.productivo.ValidarAptitudSacrificioUseCase;
import com.avicontrol.infrastructure.adapter.in.rest.sacrificio.productivo.dto.ValidacionAptitudSacrificioResponse;
import com.avicontrol.infrastructure.adapter.in.rest.sacrificio.productivo.mapper.AptitudSacrificioRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = "basicAuth")
public class AptitudSacrificioController {
  private final ValidarAptitudSacrificioUseCase caso;
  private final AptitudSacrificioRestMapper mapper;

  public AptitudSacrificioController(
      ValidarAptitudSacrificioUseCase caso, AptitudSacrificioRestMapper mapper) {
    this.caso = caso;
    this.mapper = mapper;
  }

  @PostMapping("/api/galpones/{galponId}/validacion-aptitud-sacrificio")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  @Operation(
      summary = "Validar aptitud del galpón para sacrificio productivo",
      description = "Solo administrador. Si es apto, cambia PRODUCTIVA a EN_COSECHA.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Aptitud evaluada"),
    @ApiResponse(responseCode = "400", description = "Identificador inválido"),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Rol insuficiente"),
    @ApiResponse(responseCode = "404", description = "Galpón o lote actual ausente"),
    @ApiResponse(responseCode = "409", description = "Información inconsistente"),
    @ApiResponse(responseCode = "503", description = "Módulo 1 no disponible")
  })
  public ValidacionAptitudSacrificioResponse validar(@PathVariable UUID galponId) {
    return mapper.response(caso.validar(new ValidarAptitudSacrificioCommand(galponId)));
  }
}
