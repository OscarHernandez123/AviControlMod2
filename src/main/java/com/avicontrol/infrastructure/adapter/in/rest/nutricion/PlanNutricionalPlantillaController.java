package com.avicontrol.infrastructure.adapter.in.rest.nutricion;

import com.avicontrol.application.nutricion.GestionarPlantillasUseCase;
import com.avicontrol.domain.model.catalogo.EtapaCrianza;
import com.avicontrol.domain.model.nutricion.PlanNutricionalPlantilla;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/nutricion/plantillas")
public class PlanNutricionalPlantillaController {
  private final GestionarPlantillasUseCase useCase;

  public PlanNutricionalPlantillaController(GestionarPlantillasUseCase useCase) {
    this.useCase = useCase;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('NUTRICIONISTA')")
  public PlanNutricionalPlantilla crear(@RequestBody PlantillaRequest request) {
    return useCase.crear(request.toDomain());
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('NUTRICIONISTA')")
  public PlanNutricionalPlantilla editar(
      @PathVariable UUID id, @RequestBody PlantillaRequest request) {
    return useCase.editar(id, request.toDomain());
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('NUTRICIONISTA','ADMINISTRADOR')")
  public List<PlanNutricionalPlantilla> listar() {
    return useCase.listar();
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('NUTRICIONISTA','ADMINISTRADOR')")
  public PlanNutricionalPlantilla consultar(@PathVariable UUID id) {
    return useCase.consultar(id);
  }

  public record PlantillaRequest(
      String nombre, String tipoAve, boolean esPredeterminado, List<EtapaRequest> etapas) {
    PlanNutricionalPlantilla toDomain() {
      if (etapas == null) throw new IllegalArgumentException("Las etapas son obligatorias");
      return new PlanNutricionalPlantilla(
          null,
          nombre,
          tipoAve,
          esPredeterminado,
          true,
          Instant.EPOCH,
          etapas.stream().map(EtapaRequest::toDomain).toList());
    }
  }

  public record EtapaRequest(
      EtapaCrianza etapaCrianza,
      int diaInicio,
      int diaFin,
      BigDecimal racionKgAveDia,
      EtapaCrianza tipoAlimento,
      BigDecimal pesoNominalPorBultoKg,
      UUID alimentoId) {
    PlanNutricionalPlantilla.Etapa toDomain() {
      return new PlanNutricionalPlantilla.Etapa(
          etapaCrianza,
          diaInicio,
          diaFin,
          racionKgAveDia,
          tipoAlimento,
          pesoNominalPorBultoKg,
          alimentoId);
    }
  }
}
