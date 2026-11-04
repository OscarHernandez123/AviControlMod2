package com.avicontrol.infrastructure.adapter.out.persistence.recepcion;

import com.avicontrol.domain.model.recepcion.RegistroAuditoriaRecepcion;
import com.avicontrol.domain.port.out.recepcion.AuditoriaRecepcionRepositoryPort;
import org.springframework.stereotype.Component;

@Component
class AuditoriaRecepcionPersistenceAdapter implements AuditoriaRecepcionRepositoryPort {
  private final RegistroAuditoriaRecepcionJpaRepository repository;

  AuditoriaRecepcionPersistenceAdapter(RegistroAuditoriaRecepcionJpaRepository repository) {
    this.repository = repository;
  }

  public RegistroAuditoriaRecepcion guardar(RegistroAuditoriaRecepcion registro) {
    repository.save(new RegistroAuditoriaRecepcionJpaEntity(registro));
    return registro;
  }
}
