package com.avicontrol.infrastructure.adapter.out.persistence.sanitario.enfermedad;

import com.avicontrol.domain.model.sanitario.enfermedad.Enfermedad;
import com.avicontrol.domain.port.out.sanitario.EnfermedadRepositoryPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class EnfermedadPersistenceAdapter implements EnfermedadRepositoryPort {
  private final EnfermedadJpaRepository repository;
  private final EnfermedadPersistenceMapper mapper;

  EnfermedadPersistenceAdapter(
      EnfermedadJpaRepository repository, EnfermedadPersistenceMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public Enfermedad guardar(Enfermedad enfermedad) {
    return mapper.toDomain(repository.save(mapper.toEntity(enfermedad)));
  }

  @Override
  public Optional<Enfermedad> buscarPorId(UUID id) {
    return repository.findById(id).map(mapper::toDomain);
  }
}
