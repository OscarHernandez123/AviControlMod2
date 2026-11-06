package com.avicontrol.infrastructure.adapter.in.rest.nutricion;

import com.avicontrol.application.nutricion.PlanNutricionalLoteUseCase;
import com.avicontrol.domain.model.catalogo.EtapaCrianza;
import com.avicontrol.domain.model.nutricion.AuditoriaPlanNutricional;
import com.avicontrol.domain.model.nutricion.PlanNutricionalGalpon;
import com.avicontrol.domain.port.out.nutricion.AlimentoCompatibleQueryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PlanNutricionalGalponController {
  private final PlanNutricionalLoteUseCase useCase;

  public PlanNutricionalGalponController(PlanNutricionalLoteUseCase useCase) {
    this.useCase = useCase;
  }

  @GetMapping("/galpones/{galponId}/plan-nutricional")
  @PreAuthorize("hasAnyRole('NUTRICIONISTA','ADMINISTRADOR')")
  public PlanDetalle consultar(@PathVariable UUID galponId) {
    PlanNutricionalGalpon plan = useCase.consultar(galponId);
    return new PlanDetalle(
        plan.galponId(),
        plan.loteId(),
        plan.estado(),
        plan.fechaSalidaProyectada(),
        useCase.detalle(galponId).stream()
            .map(
                d ->
                    new EtapaDetalle(
                        d.etapa().id(),
                        d.etapa().etapaCrianza(),
                        d.etapa().estado(),
                        d.etapa().diaInicio(),
                        d.etapa().diaFin(),
                        d.etapa().racionKgAveDia(),
                        d.etapa().alimentoId(),
                        d.demandaDiariaKg(),
                        d.demandaDiariaBultos(),
                        d.diasAutonomia()))
            .toList(),
        useCase.historial(plan.id()));
  }

  @PutMapping("/galpones/{galponId}/plan-nutricional/asignacion")
  @PreAuthorize("hasRole('NUTRICIONISTA')")
  public PlanNutricionalGalpon asignar(
      @PathVariable UUID galponId,
      @RequestBody AsignacionRequest request,
      Authentication authentication) {
    return useCase.asignarManual(
        galponId, request.alojamiento(galponId), request.plantillaId(), authentication.getName());
  }

  @PutMapping("/galpones/{galponId}/plan-nutricional/sustituir")
  @PreAuthorize("hasRole('NUTRICIONISTA')")
  public PlanNutricionalGalpon sustituir(
      @PathVariable UUID galponId,
      @RequestBody SustitucionRequest request,
      Authentication authentication) {
    return useCase.sustituir(
        galponId, request.plantillaId(), request.justificacion(), authentication.getName());
  }

  @GetMapping("/nutricion/alimentos-compatibles")
  @PreAuthorize("hasAnyRole('NUTRICIONISTA','ADMINISTRADOR')")
  public List<AlimentoCompatibleQueryPort.Alimento> compatibles(
      @RequestParam EtapaCrianza tipoAlimento) {
    return useCase.compatibles(tipoAlimento);
  }

  @PutMapping("/galpones/{galponId}/plan-nutricional/etapas/{etapaId}/alimento")
  @PreAuthorize("hasRole('NUTRICIONISTA')")
  public PlanNutricionalGalpon seleccionar(
      @PathVariable UUID galponId,
      @PathVariable UUID etapaId,
      @RequestBody SeleccionarAlimentoRequest request,
      Authentication authentication) {
    return useCase.seleccionarAlimento(
        galponId, etapaId, request.alimentoId(), authentication.getName());
  }

  @PutMapping("/galpones/{galponId}/plan-nutricional/etapas/{etapaId}/ajuste-duracion")
  @PreAuthorize("hasRole('NUTRICIONISTA')")
  public PlanNutricionalGalpon ajustar(
      @PathVariable UUID galponId,
      @PathVariable UUID etapaId,
      @RequestBody AjusteDuracionRequest request,
      Authentication authentication) {
    return useCase.ajustarDuracion(
        galponId,
        etapaId,
        request.nuevoDiaFin(),
        request.motivo(),
        request.justificacion(),
        authentication.getName());
  }

  public record AsignacionRequest(
      UUID plantillaId,
      UUID loteId,
      String codigoLote,
      String tipoAve,
      LocalDate fechaIngreso,
      int edadIngresoDias,
      int poblacionViva,
      String justificacion) {
    PlanNutricionalLoteUseCase.Alojamiento alojamiento(UUID galponId) {
      return new PlanNutricionalLoteUseCase.Alojamiento(
          galponId, loteId, codigoLote, tipoAve, fechaIngreso, edadIngresoDias, poblacionViva);
    }
  }

  public record SustitucionRequest(UUID plantillaId, String justificacion) {}

  public record SeleccionarAlimentoRequest(UUID alimentoId) {}

  public record AjusteDuracionRequest(int nuevoDiaFin, String motivo, String justificacion) {}

  public record PlanDetalle(
      UUID galponId,
      UUID loteId,
      String estado,
      LocalDate fechaSalidaProyectada,
      List<EtapaDetalle> etapas,
      List<AuditoriaPlanNutricional> historialAjustes) {}

  public record EtapaDetalle(
      UUID id,
      EtapaCrianza etapaCrianza,
      String estadoEtapa,
      int diaInicio,
      int diaFin,
      BigDecimal racionKgAveDia,
      UUID alimentoId,
      BigDecimal demandaDiariaKg,
      BigDecimal demandaDiariaBultos,
      BigDecimal diasAutonomia) {}
}
