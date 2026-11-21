package com.avicontrol.infrastructure.adapter.in.rest.modulo3;

import com.avicontrol.application.sacrificio.productivo.MarcarResultadoUtilizadoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = "basicAuth")
public class ConfirmacionResultadoModulo3Controller {
  public static final String RUTA = "/api/integracion/modulo3/resultados-sacrificio/utilizado";
  private final MarcarResultadoUtilizadoUseCase confirmar;

  public ConfirmacionResultadoModulo3Controller(MarcarResultadoUtilizadoUseCase confirmar) {
    this.confirmar = confirmar;
  }

  @PostMapping(RUTA)
  @PreAuthorize("hasRole('MODULO3')")
  @Operation(summary = "Confirmar por REST que Módulo 3 utilizó un resultado de sacrificio")
  public ConfirmacionResponse confirmar(
      @Valid @RequestBody ConfirmarResultadoUtilizadoRequest aviso) {
    if (!aviso.eventVersion().isIntegralNumber()
        || !aviso.eventVersion().canConvertToInt()
        || aviso.eventVersion().intValue() != 1) {
      throw new IllegalArgumentException("eventVersion debe ser el entero 1");
    }
    if (!aviso.aggregateId().equals(aviso.payload().resultadoId())) {
      throw new IllegalArgumentException("aggregateId debe coincidir con payload.resultadoId");
    }
    confirmar.confirmar(aviso.eventId(), aviso.payload().resultadoId(), aviso.payload().loteId());
    return new ConfirmacionResponse(
        aviso.eventId(), aviso.payload().resultadoId(), aviso.payload().loteId(), "UTILIZADO");
  }

  public record ConfirmacionResponse(UUID eventId, UUID resultadoId, UUID loteId, String estado) {}
}
