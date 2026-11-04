package com.avicontrol.infrastructure.adapter.out.internal.catalogo;

import com.avicontrol.application.catalogo.port.*;
import com.avicontrol.domain.model.catalogo.*;
import com.avicontrol.domain.port.out.catalogo.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
class CatalogoQueryAdapter implements AlimentoCatalogoQueryPort, MedicamentoCatalogoQueryPort {
  private final AlimentoRepositoryPort alimentos;
  private final MedicamentoRepositoryPort medicamentos;

  CatalogoQueryAdapter(AlimentoRepositoryPort alimentos, MedicamentoRepositoryPort medicamentos) {
    this.alimentos = alimentos;
    this.medicamentos = medicamentos;
  }

  public Optional<Alimento> buscarAlimentoActivo(UUID id) {
    return alimentos.buscarPorId(id).filter(a -> a.estado() == EstadoProductoCatalogo.ACTIVO);
  }

  public Optional<Medicamento> buscarMedicamentoActivo(UUID id) {
    return medicamentos.buscarPorId(id).filter(m -> m.estado() == EstadoProductoCatalogo.ACTIVO);
  }
}
