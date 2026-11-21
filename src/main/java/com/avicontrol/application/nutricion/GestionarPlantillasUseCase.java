package com.avicontrol.application.nutricion;

import com.avicontrol.domain.model.nutricion.PlanNutricionalPlantilla;
import com.avicontrol.domain.port.out.nutricion.AlimentoCompatibleQueryPort;
import com.avicontrol.domain.port.out.nutricion.PlanNutricionalPlantillaRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GestionarPlantillasUseCase {
  private final PlanNutricionalPlantillaRepositoryPort repositorio;
  private final Clock clock;
  private final AlimentoCompatibleQueryPort alimentos;

  public GestionarPlantillasUseCase(
      PlanNutricionalPlantillaRepositoryPort repositorio,
      AlimentoCompatibleQueryPort alimentos,
      Clock clock) {
    this.repositorio = repositorio;
    this.alimentos = alimentos;
    this.clock = clock;
  }

  @Transactional
  public PlanNutricionalPlantilla crear(PlanNutricionalPlantilla entrada) {
    validarAlimentos(entrada);
    PlanNutricionalPlantilla nueva =
        new PlanNutricionalPlantilla(
            UUID.randomUUID(),
            entrada.nombre(),
            entrada.tipoAve(),
            entrada.esPredeterminado(),
            true,
            Instant.now(clock),
            entrada.etapas());
    repositorio.guardar(nueva);
    return nueva;
  }

  @Transactional
  public PlanNutricionalPlantilla editar(UUID id, PlanNutricionalPlantilla entrada) {
    validarAlimentos(entrada);
    PlanNutricionalPlantilla anterior = consultar(id);
    if (!anterior.tipoAve().equals(entrada.tipoAve()))
      throw new IllegalArgumentException("El tipo de ave de una plantilla no se puede cambiar");
    PlanNutricionalPlantilla editada =
        new PlanNutricionalPlantilla(
            id,
            entrada.nombre(),
            anterior.tipoAve(),
            entrada.esPredeterminado(),
            anterior.activo(),
            anterior.fechaCreacion(),
            entrada.etapas());
    repositorio.guardar(editada);
    return editada;
  }

  public PlanNutricionalPlantilla consultar(UUID id) {
    return repositorio
        .buscar(id)
        .orElseThrow(() -> new NoSuchElementException("Plantilla no encontrada"));
  }

  public List<PlanNutricionalPlantilla> listar() {
    return repositorio.listar();
  }

  private void validarAlimentos(PlanNutricionalPlantilla plantilla) {
    for (var etapa : plantilla.etapas()) {
      if (etapa.alimentoId() == null) continue;
      var alimento =
          alimentos
              .buscar(etapa.alimentoId())
              .orElseThrow(
                  () -> new IllegalArgumentException("Alimento de plantilla no disponible"));
      if (alimento.tipoAlimento() != etapa.tipoAlimento()
          || alimento.pesoNominalPorBultoKg().compareTo(etapa.pesoNominalPorBultoKg()) != 0)
        throw new IllegalArgumentException(
            "Alimento incompatible con tipo o presentación de etapa");
    }
  }
}
