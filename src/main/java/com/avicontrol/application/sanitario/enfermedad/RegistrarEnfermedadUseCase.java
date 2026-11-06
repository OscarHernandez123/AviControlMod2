package com.avicontrol.application.sanitario.enfermedad;

import com.avicontrol.application.sanitario.enfermedad.result.EnfermedadResult;
import com.avicontrol.domain.model.sanitario.enfermedad.Enfermedad;
import com.avicontrol.domain.port.out.sanitario.enfermedad.EnfermedadRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class RegistrarEnfermedadUseCase {
  private final EnfermedadRepositoryPort repositorio;
  private final Clock clock;

  public RegistrarEnfermedadUseCase(EnfermedadRepositoryPort repositorio, Clock clock) {
    this.repositorio = repositorio;
    this.clock = clock;
  }

  @Transactional
  public EnfermedadResult registrar(RegistrarEnfermedadCommand comando) {
    if (comando == null) throw new IllegalArgumentException("Solicitud obligatoria");
    Instant ahora = Instant.now(clock);
    Enfermedad enfermedad =
        new Enfermedad(
            UUID.randomUUID(),
            comando.nombre(),
            comando.nivelRiesgo(),
            comando.tipo(),
            comando.descripcionClinica(),
            comando.requiereSacrificioSanitario(),
            0,
            ahora,
            ahora);
    repositorio.crear(enfermedad);
    return EnfermedadResult.de(enfermedad);
  }
}
