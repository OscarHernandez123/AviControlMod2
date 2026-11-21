package com.avicontrol.infrastructure.adapter.out.persistence.recepcion;

import com.avicontrol.application.recepcion.AuditoriaRecepcionPort;
import com.avicontrol.domain.model.recepcion.RecepcionAlimento;
import com.avicontrol.domain.model.recepcion.RecepcionMedicamento;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

@Repository
public class AuditoriaRecepcionPersistenceAdapter implements AuditoriaRecepcionPort {
  private final AuditoriaRecepcionJpaRepository repository;
  private final ObjectMapper mapper;
  private final Clock clock;

  public AuditoriaRecepcionPersistenceAdapter(
      AuditoriaRecepcionJpaRepository repository, ObjectMapper mapper, Clock clock) {
    this.repository = repository;
    this.mapper = mapper;
    this.clock = clock;
  }

  @Override
  public void alimento(RecepcionAlimento antes, RecepcionAlimento despues, String actor) {
    registrar(antes.id(), "ALIMENTO", antes, despues, actor);
  }

  @Override
  public void medicamento(RecepcionMedicamento antes, RecepcionMedicamento despues, String actor) {
    registrar(antes.id(), "MEDICAMENTO", antes, despues, actor);
  }

  private void registrar(UUID id, String tipo, Object antes, Object despues, String actor) {
    repository.saveAndFlush(
        new AuditoriaRecepcionEntity(
            id,
            tipo,
            actor,
            Instant.now(clock),
            mapper.writeValueAsString(antes),
            mapper.writeValueAsString(despues)));
  }
}
