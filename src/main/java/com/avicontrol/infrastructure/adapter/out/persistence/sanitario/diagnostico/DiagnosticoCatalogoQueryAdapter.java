package com.avicontrol.infrastructure.adapter.out.persistence.sanitario.diagnostico;

import com.avicontrol.domain.model.sanitario.enfermedad.Enfermedad;
import com.avicontrol.domain.model.sanitario.medicacion.Medicacion;
import com.avicontrol.domain.port.out.sanitario.diagnostico.EnfermedadQueryPort;
import com.avicontrol.domain.port.out.sanitario.diagnostico.MedicacionQueryPort;
import com.avicontrol.domain.port.out.sanitario.enfermedad.EnfermedadRepositoryPort;
import com.avicontrol.domain.port.out.sanitario.medicacion.MedicacionRepositoryPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class DiagnosticoCatalogoQueryAdapter implements EnfermedadQueryPort, MedicacionQueryPort {
  private final EnfermedadRepositoryPort enfermedades;
  private final MedicacionRepositoryPort medicaciones;

  public DiagnosticoCatalogoQueryAdapter(
      EnfermedadRepositoryPort enfermedades, MedicacionRepositoryPort medicaciones) {
    this.enfermedades = enfermedades;
    this.medicaciones = medicaciones;
  }

  @Override
  public Optional<Enfermedad> buscarEnfermedad(UUID id) {
    return enfermedades.buscar(id);
  }

  @Override
  public Optional<Medicacion> buscarMedicacion(UUID id) {
    return medicaciones.buscar(id);
  }
}
