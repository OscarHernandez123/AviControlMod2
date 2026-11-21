package com.avicontrol.infrastructure.adapter.out.persistence.sanitario.medicacion;

import com.avicontrol.domain.port.out.sanitario.enfermedad.EnfermedadRepositoryPort;
import com.avicontrol.domain.port.out.sanitario.medicacion.EnfermedadQueryPort;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class EnfermedadDisponibleAdapter implements EnfermedadQueryPort {
  private final EnfermedadRepositoryPort enfermedades;

  public EnfermedadDisponibleAdapter(EnfermedadRepositoryPort enfermedades) {
    this.enfermedades = enfermedades;
  }

  @Override
  public boolean disponible(UUID id) {
    return enfermedades.buscar(id).isPresent();
  }
}
