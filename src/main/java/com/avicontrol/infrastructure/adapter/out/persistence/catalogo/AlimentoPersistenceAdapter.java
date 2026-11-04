package com.avicontrol.infrastructure.adapter.out.persistence.catalogo;

import com.avicontrol.domain.model.catalogo.Alimento;
import com.avicontrol.domain.port.out.catalogo.AlimentoRepositoryPort;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
class AlimentoPersistenceAdapter implements AlimentoRepositoryPort {
  private final AlimentoJpaRepository repo;

  AlimentoPersistenceAdapter(AlimentoJpaRepository repo) {
    this.repo = repo;
  }

  public Alimento guardar(Alimento a) {
    return map(repo.save(new AlimentoJpaEntity(a)));
  }

  public Optional<Alimento> buscarPorId(UUID id) {
    return repo.findById(id).map(this::map);
  }

  public List<Alimento> listar() {
    return repo.findAll().stream().map(this::map).toList();
  }

  private Alimento map(AlimentoJpaEntity e) {
    return new Alimento(
        e.id,
        e.nombreComercial,
        e.marca,
        e.tipo,
        e.pesoNominalBultoKg,
        e.duracionRecomendadaEtapaDias,
        e.estado);
  }
}
