package com.avicontrol.application.sanitario.medicacion;

import com.avicontrol.application.sanitario.medicacion.result.MedicacionResult;
import com.avicontrol.domain.exception.sanitario.medicacion.EnfermedadNoDisponibleException;
import com.avicontrol.domain.exception.sanitario.medicacion.MedicacionNoEncontradaException;
import com.avicontrol.domain.exception.sanitario.medicacion.MedicamentoNoDisponibleException;
import com.avicontrol.domain.model.sanitario.medicacion.Medicacion;
import com.avicontrol.domain.port.out.sanitario.medicacion.EnfermedadQueryPort;
import com.avicontrol.domain.port.out.sanitario.medicacion.MedicacionRepositoryPort;
import com.avicontrol.domain.port.out.sanitario.medicacion.MedicamentoCatalogoQueryPort;
import java.time.Clock;
import java.time.Instant;
import org.springframework.transaction.annotation.Transactional;

public class EditarMedicacionUseCase {
  private final MedicacionRepositoryPort repositorio;
  private final EnfermedadQueryPort enfermedades;
  private final MedicamentoCatalogoQueryPort medicamentos;
  private final Clock clock;

  public EditarMedicacionUseCase(
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
  public MedicacionResult editar(EditarMedicacionCommand comando) {
    if (comando == null || comando.id() == null)
      throw new IllegalArgumentException("Identificador de medicación obligatorio");
    Medicacion anterior =
        repositorio.buscar(comando.id()).orElseThrow(MedicacionNoEncontradaException::new);
    Medicacion actualizada =
        anterior.actualizar(
            comando.enfermedadId(),
            comando.medicamentoId(),
            comando.dosis(),
            comando.duracionDias(),
            comando.descripcion(),
            comando.nombreMedicamento() == null && comando.principioActivo() == null
                ? anterior.nombreMedicamento()
                : comando.nombreMedicamento(),
            comando.nombreMedicamento() == null && comando.principioActivo() == null
                ? anterior.principioActivo()
                : comando.principioActivo(),
            comando.diasAplicacion() == null ? anterior.diasAplicacion() : comando.diasAplicacion(),
            Instant.now(clock));
    if (!enfermedades.disponible(actualizada.enfermedadId()))
      throw new EnfermedadNoDisponibleException();
    if (!medicamentos.disponible(actualizada.medicamentoId()))
      throw new MedicamentoNoDisponibleException();
    if (!repositorio.actualizar(anterior, actualizada))
      throw new IllegalStateException("La medicación cambió durante la edición");
    return MedicacionResult.de(actualizada);
  }
}
