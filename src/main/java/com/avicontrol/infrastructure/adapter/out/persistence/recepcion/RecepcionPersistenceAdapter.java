package com.avicontrol.infrastructure.adapter.out.persistence.recepcion;

import com.avicontrol.domain.model.recepcion.*;
import com.avicontrol.domain.port.out.recepcion.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
class RecepcionPersistenceAdapter
    implements RecepcionAlimentoRepositoryPort, RecepcionMedicamentoRepositoryPort {
  private final RecepcionAlimentoJpaRepository a;
  private final RecepcionMedicamentoJpaRepository m;

  RecepcionPersistenceAdapter(
      RecepcionAlimentoJpaRepository a, RecepcionMedicamentoJpaRepository m) {
    this.a = a;
    this.m = m;
  }

  public RecepcionAlimento guardar(RecepcionAlimento r) {
    return aa(a.save(new RecepcionAlimentoJpaEntity(r)));
  }

  public Optional<RecepcionAlimento> buscarPorId(UUID id) {
    return a.findById(id).map(this::aa);
  }

  public List<RecepcionAlimento> listar() {
    return a.findAll().stream().map(this::aa).toList();
  }

  public RecepcionMedicamento guardar(RecepcionMedicamento r) {
    return mm(m.save(new RecepcionMedicamentoJpaEntity(r)));
  }

  public Optional<RecepcionMedicamento> buscarMedicamentoPorId(UUID id) {
    return m.findById(id).map(this::mm);
  }

  public List<RecepcionMedicamento> listarMedicamentos() {
    return m.findAll().stream().map(this::mm).toList();
  }

  private RecepcionAlimento aa(RecepcionAlimentoJpaEntity e) {
    return new RecepcionAlimento(
        e.id,
        e.alimentoId,
        e.nombreAlimentoHistorico,
        e.tipoAlimentoHistorico,
        e.codigoLote,
        e.fechaIngreso,
        e.fechaVencimiento,
        e.cantidadBultos,
        e.pesoPorBultoKg,
        e.precioNetoPorBulto,
        e.porcentajeImpuesto,
        e.estado);
  }

  private RecepcionMedicamento mm(RecepcionMedicamentoJpaEntity e) {
    return new RecepcionMedicamento(
        e.id,
        e.medicamentoId,
        e.nombreMedicamentoHistorico,
        e.codigoLote,
        e.presentacion,
        e.cantidad,
        e.contenidoPorPresentacion,
        e.unidadMedida,
        e.fechaIngreso,
        e.fechaVencimiento,
        e.precioNetoPorPresentacion,
        e.porcentajeImpuesto,
        e.estado);
  }
}
