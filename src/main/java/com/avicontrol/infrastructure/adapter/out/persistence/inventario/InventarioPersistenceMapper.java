package com.avicontrol.infrastructure.adapter.out.persistence.inventario;

import com.avicontrol.domain.model.inventario.*;
import org.springframework.stereotype.Component;

@Component
class InventarioPersistenceMapper {
  MovimientoAlimentoJpaEntity toEntity(MovimientoAlimento m) {
    return new MovimientoAlimentoJpaEntity(
        m.id(), m.recepcionId(), m.tipo(), m.cantidadKg(), m.estado(), m.ocurridoEn(), m.actorId());
  }

  MovimientoAlimento toDomain(MovimientoAlimentoJpaEntity e) {
    return new MovimientoAlimento(
        e.id, e.recepcionId, e.tipo, e.cantidadKg, e.estado, e.ocurridoEn, e.actorId);
  }

  MovimientoMedicamentoJpaEntity toEntity(MovimientoMedicamento m) {
    return new MovimientoMedicamentoJpaEntity(
        m.id(),
        m.recepcionId(),
        m.tipo(),
        m.cantidadUnidadBase(),
        m.unidadBase(),
        m.estado(),
        m.ocurridoEn(),
        m.actorId());
  }

  MovimientoMedicamento toDomain(MovimientoMedicamentoJpaEntity e) {
    return new MovimientoMedicamento(
        e.id,
        e.recepcionId,
        e.tipo,
        e.cantidadUnidadBase,
        e.unidadBase,
        e.estado,
        e.ocurridoEn,
        e.actorId);
  }
}
