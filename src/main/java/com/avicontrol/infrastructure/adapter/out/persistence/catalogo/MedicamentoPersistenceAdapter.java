package com.avicontrol.infrastructure.adapter.out.persistence.catalogo;

import com.avicontrol.domain.model.catalogo.Medicamento;
import com.avicontrol.domain.port.out.catalogo.MedicamentoRepositoryPort;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
class MedicamentoPersistenceAdapter implements MedicamentoRepositoryPort {
  private final MedicamentoJpaRepository repo;

  MedicamentoPersistenceAdapter(MedicamentoJpaRepository repo) {
    this.repo = repo;
  }

  public Medicamento guardar(Medicamento m) {
    return map(repo.save(new MedicamentoJpaEntity(m)));
  }

  public Optional<Medicamento> buscarPorId(UUID id) {
    return repo.findById(id).map(this::map);
  }

  public List<Medicamento> listar() {
    return repo.findAll().stream().map(this::map).toList();
  }

  private Medicamento map(MedicamentoJpaEntity e) {
    return new Medicamento(
        e.id, e.nombreComercial, e.presentacionCompatible, e.unidadBase, e.estado);
  }
}
