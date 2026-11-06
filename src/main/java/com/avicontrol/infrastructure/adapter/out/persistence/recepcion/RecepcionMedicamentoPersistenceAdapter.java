package com.avicontrol.infrastructure.adapter.out.persistence.recepcion;

import com.avicontrol.domain.exception.recepcion.RecursoNoEncontradoException;
import com.avicontrol.domain.model.recepcion.FiltroRecepciones;
import com.avicontrol.domain.model.recepcion.PaginaRecepciones;
import com.avicontrol.domain.model.recepcion.RecepcionMedicamento;
import com.avicontrol.domain.port.out.recepcion.RecepcionMedicamentoRepositoryPort;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public class RecepcionMedicamentoPersistenceAdapter implements RecepcionMedicamentoRepositoryPort {
  private final RecepcionMedicamentoJpaRepository repository;

  public RecepcionMedicamentoPersistenceAdapter(RecepcionMedicamentoJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  public void guardar(RecepcionMedicamento recepcion) {
    repository.saveAndFlush(new RecepcionMedicamentoEntity(recepcion));
  }

  @Override
  public Optional<RecepcionMedicamento> buscar(UUID id) {
    return repository.findById(id).map(RecepcionMedicamentoEntity::toDomain);
  }

  @Override
  public Optional<RecepcionMedicamento> bloquear(UUID id) {
    return repository.bloquear(id).map(RecepcionMedicamentoEntity::toDomain);
  }

  @Override
  public void actualizar(RecepcionMedicamento recepcion) {
    RecepcionMedicamentoEntity entity =
        repository
            .findById(recepcion.id())
            .orElseThrow(() -> new RecursoNoEncontradoException("Recepción de medicamento"));
    entity.actualizar(recepcion);
    repository.saveAndFlush(entity);
  }

  @Override
  public PaginaRecepciones<RecepcionMedicamento> listar(FiltroRecepciones filtro) {
    Specification<RecepcionMedicamentoEntity> spec =
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
          if (filtro.presentacion() != null && !filtro.presentacion().isBlank())
            predicate =
                cb.and(
                    predicate,
                    cb.like(
                        cb.lower(root.get("presentacion")),
                        "%" + filtro.presentacion().trim().toLowerCase() + "%"));
          if (filtro.busqueda() != null && !filtro.busqueda().isBlank()) {
            String text = "%" + filtro.busqueda().trim().toLowerCase() + "%";
            predicate =
                cb.and(
                    predicate,
                    cb.or(
                        cb.like(cb.lower(root.get("codigoLote")), text),
                        cb.like(cb.lower(root.get("nombreMedicamento")), text)));
          }
          return predicate;
        };
    Page<RecepcionMedicamentoEntity> page =
        repository.findAll(
            spec,
            PageRequest.of(
                filtro.pagina(),
                filtro.tamano(),
                Sort.by(Sort.Direction.DESC, "fechaIngreso", "id")));
    return new PaginaRecepciones<>(
        page.map(RecepcionMedicamentoEntity::toDomain).getContent(),
        page.getTotalElements(),
        filtro.pagina(),
        filtro.tamano());
  }

  @Override
  public long contarMes(LocalDate desde, LocalDate hasta) {
    return repository.contarMes(desde, hasta);
  }
}
