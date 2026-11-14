package com.avicontrol.domain.port.out.sanitario.medicacion;

import com.avicontrol.domain.model.sanitario.medicacion.Medicacion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MedicacionRepositoryPort {
  void crear(Medicacion medicacion);

  Optional<Medicacion> buscar(UUID id);

  List<Medicacion> listar();

  boolean actualizar(Medicacion anterior, Medicacion actualizada);
}
