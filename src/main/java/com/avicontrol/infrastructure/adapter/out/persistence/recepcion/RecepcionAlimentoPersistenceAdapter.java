package com.avicontrol.infrastructure.adapter.out.persistence.recepcion;

import com.avicontrol.domain.exception.recepcion.RecursoNoEncontradoException;
import com.avicontrol.domain.model.recepcion.FiltroRecepciones;
import com.avicontrol.domain.model.recepcion.PaginaRecepciones;
import com.avicontrol.domain.model.recepcion.RecepcionAlimento;
import com.avicontrol.domain.port.out.recepcion.RecepcionAlimentoRepositoryPort;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public class RecepcionAlimentoPersistenceAdapter implements RecepcionAlimentoRepositoryPort {
  private final RecepcionAlimentoJpaRepository repository;

  public RecepcionAlimentoPersistenceAdapter(RecepcionAlimentoJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  public void guardar(RecepcionAlimento recepcion) {
    repository.saveAndFlush(new RecepcionAlimentoEntity(recepcion));
  }

  @Override
  public Optional<RecepcionAlimento> buscar(UUID id) {
    return repository.findById(id).map(RecepcionAlimentoEntity::toDomain);
  }

  @Override
  public Optional<RecepcionAlimento> bloquear(UUID id) {
    return repository.bloquear(id).map(RecepcionAlimentoEntity::toDomain);
  }

  @Override
  public void actualizar(RecepcionAlimento recepcion) {
    RecepcionAlimentoEntity entity =
        repository
            .findById(recepcion.id())
            .orElseThrow(() -> new RecursoNoEncontradoException("Recepción de alimento"));
    entity.actualizar(recepcion);
    repository.saveAndFlush(entity);
  }

  @Override
  public PaginaRecepciones<RecepcionAlimento> listar(FiltroRecepciones filtro) {
    Specification<RecepcionAlimentoEntity> spec =
        (root, query, cb) -> {
          Predicate predicate = cb.conjunction();
          if (filtro.desde() != null)
            predicate =
                cb.and(
                    predicate, cb.greaterThanOrEqualTo(root.get("fechaIngreso"), filtro.desde()));
          if (filtro.hasta() != null)
            predicate =
                cb.and(predicate, cb.lessThanOrEqualTo(root.get("fechaIngreso"), filtro.hasta()));
          if (filtro.estado() != null)
            predicate = cb.and(predicate, cb.equal(root.get("estado"), filtro.estado().name()));
          if (filtro.busqueda() != null && !filtro.busqueda().isBlank()) {
            String text = filtro.busqueda().trim().toLowerCase();
            Predicate lot = cb.like(cb.lower(root.get("codigoLote")), "%" + text + "%");
            try {
              predicate =
                  cb.and(predicate, cb.or(lot, cb.equal(root.get("id"), UUID.fromString(text))));
            } catch (IllegalArgumentException ignored) {
              predicate = cb.and(predicate, lot);
            }
          }
          return predicate;
        };
    Page<RecepcionAlimentoEntity> page =
        repository.findAll(
            spec,
            PageRequest.of(
                filtro.pagina(),
                filtro.tamano(),
                Sort.by(Sort.Direction.DESC, "fechaIngreso", "id")));
    return new PaginaRecepciones<>(
        page.map(RecepcionAlimentoEntity::toDomain).getContent(),
        page.getTotalElements(),
        filtro.pagina(),
        filtro.tamano());
  }

  @Override
  public long contarMes(LocalDate desde, LocalDate hasta) {
    return repository.contarMes(desde, hasta);
  }

  @Override
  public BigDecimal pesoMes(LocalDate desde, LocalDate hasta) {
    return repository.pesoMes(desde, hasta);
  }
}
