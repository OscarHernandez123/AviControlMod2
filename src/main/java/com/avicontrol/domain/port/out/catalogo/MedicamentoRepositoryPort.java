package com.avicontrol.domain.port.out.catalogo;

import com.avicontrol.domain.model.catalogo.Medicamento;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MedicamentoRepositoryPort {
  Medicamento guardar(Medicamento medicamento);

  Optional<Medicamento> buscarPorId(UUID id);

  List<Medicamento> listar();
}
