package com.avicontrol.application.sanitario.enfermedad;

import com.avicontrol.application.sanitario.enfermedad.result.EnfermedadResult;
import com.avicontrol.domain.exception.sanitario.enfermedad.EnfermedadNoEncontradaException;
import com.avicontrol.domain.model.sanitario.enfermedad.Enfermedad;
import com.avicontrol.domain.port.out.sanitario.enfermedad.EnfermedadRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import org.springframework.transaction.annotation.Transactional;

public class EditarEnfermedadUseCase {
  private final EnfermedadRepositoryPort repositorio;
  private final Clock clock;

  public EditarEnfermedadUseCase(EnfermedadRepositoryPort repositorio, Clock clock) {
    this.repositorio = repositorio;
    this.clock = clock;
  }

  @Transactional
  public EnfermedadResult editar(EditarEnfermedadCommand comando) {
    if (comando == null || comando.id() == null)
      throw new IllegalArgumentException("Identificador de enfermedad obligatorio");
    Enfermedad anterior =
        repositorio.buscar(comando.id()).orElseThrow(EnfermedadNoEncontradaException::new);
    Enfermedad actualizada =
        anterior.actualizar(
            comando.nombre(),
            comando.nivelRiesgo(),
            comando.tipo(),
            comando.descripcionClinica(),
            comando.requiereSacrificioSanitario(),
            Instant.now(clock));
    if (!repositorio.actualizar(anterior, actualizada))
      throw new IllegalStateException("La enfermedad cambió durante la edición");
    return EnfermedadResult.de(actualizada);
  }
}
