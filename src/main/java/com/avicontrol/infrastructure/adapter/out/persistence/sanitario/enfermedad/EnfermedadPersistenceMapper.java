package com.avicontrol.infrastructure.adapter.out.persistence.sanitario.enfermedad;

import com.avicontrol.domain.model.sanitario.enfermedad.Enfermedad;
import org.springframework.stereotype.Component;

@Component
class EnfermedadPersistenceMapper {
  EnfermedadJpaEntity toEntity(Enfermedad e) {
    return new EnfermedadJpaEntity(
        e.id(),
        e.nombre(),
        e.nivelRiesgo(),
        e.tipo(),
        e.descripcionClinica(),
        e.requiereSacrificioSanitario());
  }

  Enfermedad toDomain(EnfermedadJpaEntity e) {
    return new Enfermedad(
        e.getId(),
        e.getNombre(),
        e.getNivelRiesgo(),
        e.getTipo(),
        e.getDescripcionClinica(),
        e.isRequiereSacrificioSanitario());
  }
}
