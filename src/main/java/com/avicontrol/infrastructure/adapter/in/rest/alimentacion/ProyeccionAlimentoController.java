package com.avicontrol.infrastructure.adapter.in.rest.alimentacion;

import com.avicontrol.application.alimentacion.ProyeccionAlimentoUseCase;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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
public class ProyeccionAlimentoController {
  private final ProyeccionAlimentoUseCase useCase;

  public ProyeccionAlimentoController(ProyeccionAlimentoUseCase useCase) {
    this.useCase = useCase;
  }

  @GetMapping("/lotes/{loteId}/alimento-requerido")
  @PreAuthorize("hasAnyRole('ADMINISTRADOR','NUTRICIONISTA','FINANZAS')")
  public RequerimientoResponse consultar(@PathVariable UUID loteId) {
    var requerimiento = useCase.consultarLote(loteId);
    return new RequerimientoResponse(
        requerimiento.codigoLote(),
        requerimiento.loteId(),
        requerimiento.galponId(),
        requerimiento.estadoCiclo(),
        requerimiento.fechaConsulta(),
        requerimiento.etapas().stream().map(this::detalle).toList());
  }

  private EtapaResponse detalle(ProyeccionAlimentoUseCase.Proyeccion p) {
    var historial = useCase.historial(p.id());
    var ultimo = historial.isEmpty() ? null : historial.get(historial.size() - 1);
    return new EtapaResponse(
        p.id(),
        p.etapaCrianza(),
        p.tipoAlimento(),
        useCase.nombreAlimento(p.alimentoId()),
        p.pesoNominalPorBultoKg(),
        p.poblacionInicioEtapa(),
        p.cuotaKgAveDia(),
        p.estado(),
        p.proyeccionKg(),
        p.costoUnitarioKg(),
        p.fuenteCosto(),
        p.fechaCapturaCosto(),
        p.advertenciaCosto(),
        p.diasBase(),
        p.diasProrroga(),
        p.diasBase() + p.diasProrroga(),
        ultimo == null ? null : ultimo.motivo(),
        ultimo == null ? null : ultimo.justificacion(),
        historial);
  }

  @PostMapping("/proyecciones/{proyeccionId}/completar-costo")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public CompletarCostoResponse completar(
      @PathVariable UUID proyeccionId,
      @RequestBody CompletarCostoRequest request,
      Authentication authentication) {
    var p =
        useCase.completarCosto(
            proyeccionId,
            request.costoUnitarioKg(),
            request.justificacion(),
            authentication.getName());
    return new CompletarCostoResponse(
        p.id(), p.costoUnitarioKg(), p.fuenteCosto(), p.costoCompletadoManualmente());
  }

  @GetMapping("/administracion/balance-abastecimiento")
  @PreAuthorize("hasAnyRole('ADMINISTRADOR','NUTRICIONISTA')")
  public BalanceResponse balance() {
    return new BalanceResponse(useCase.balance());
  }

  public record CompletarCostoRequest(BigDecimal costoUnitarioKg, String justificacion) {}

  public record CompletarCostoResponse(
      UUID proyeccionId,
      BigDecimal costoUnitarioKg,
      String fuenteCosto,
      boolean costoCompletadoManualmente) {}

  public record BalanceResponse(List<ProyeccionAlimentoUseCase.Balance> alimentos) {}

  public record RequerimientoResponse(
      String codigoLote,
      UUID loteId,
      UUID galponId,
      String estadoCiclo,
      Instant fechaConsulta,
      List<EtapaResponse> etapas) {}

  public record EtapaResponse(
      UUID proyeccionId,
      String etapaCrianza,
      String tipoAlimento,
      String alimentoComercial,
      BigDecimal pesoNominalPorBultoKg,
      int poblacionInicioEtapa,
      BigDecimal cuotaKgAveDia,
      String estadoEtapa,
      BigDecimal kilogramosProyectados,
      BigDecimal costoUnitarioKg,
      String fuenteCosto,
      Instant fechaCapturaCosto,
      String advertenciaCosto,
      int diasBase,
      int diasProrroga,
      int diasEfectivos,
      String motivoProrroga,
      String justificacion,
      List<ProyeccionAlimentoUseCase.HistorialAjuste> historialAjustes) {}
}
