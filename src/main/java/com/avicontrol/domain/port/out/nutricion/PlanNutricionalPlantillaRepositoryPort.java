package com.avicontrol.domain.port.out.nutricion;

import com.avicontrol.domain.model.nutricion.PlanNutricionalPlantilla;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanNutricionalPlantillaRepositoryPort {
  void guardar(PlanNutricionalPlantilla plantilla);

  Optional<PlanNutricionalPlantilla> buscar(UUID id);

  Optional<PlanNutricionalPlantilla> predeterminada(String tipoAve);

  List<PlanNutricionalPlantilla> listar();
}
