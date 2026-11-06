package com.avicontrol.application.sanitario.medicacion;

import com.avicontrol.application.sanitario.medicacion.result.MedicacionResult;
import com.avicontrol.domain.exception.sanitario.medicacion.EnfermedadNoDisponibleException;
import com.avicontrol.domain.exception.sanitario.medicacion.MedicamentoNoDisponibleException;
import com.avicontrol.domain.model.sanitario.medicacion.Medicacion;
import com.avicontrol.domain.port.out.sanitario.medicacion.EnfermedadQueryPort;
import com.avicontrol.domain.port.out.sanitario.medicacion.MedicacionRepositoryPort;
import com.avicontrol.domain.port.out.sanitario.medicacion.MedicamentoCatalogoQueryPort;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class RegistrarMedicacionUseCase {
  private final MedicacionRepositoryPort repositorio;
  private final EnfermedadQueryPort enfermedades;
  private final MedicamentoCatalogoQueryPort medicamentos;
  private final Clock clock;

  public RegistrarMedicacionUseCase(
      MedicacionRepositoryPort repositorio,
      EnfermedadQueryPort enfermedades,
      MedicamentoCatalogoQueryPort medicamentos,
      Clock clock) {
    this.repositorio = repositorio;
    this.enfermedades = enfermedades;
    this.medicamentos = medicamentos;
    this.clock = clock;
  }

  @Transactional
  public MedicacionResult registrar(RegistrarMedicacionCommand comando) {
    if (comando == null) throw new IllegalArgumentException("Solicitud obligatoria");
    Instant ahora = Instant.now(clock);
    Medicacion medicacion =
        new Medicacion(
            UUID.randomUUID(),
            comando.enfermedadId(),
            comando.medicamentoId(),
            comando.dosis(),
            comando.duracionDias(),
            comando.descripcion(),
            0,
            ahora,
            ahora);
    if (!enfermedades.disponible(medicacion.enfermedadId()))
      throw new EnfermedadNoDisponibleException();
    if (!medicamentos.disponible(medicacion.medicamentoId()))
      throw new MedicamentoNoDisponibleException();
    repositorio.crear(medicacion);
    return MedicacionResult.de(medicacion);
  }
}
