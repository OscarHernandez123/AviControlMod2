package com.avicontrol.domain.port.out.medicamento;

import com.avicontrol.domain.model.medicamento.ConsumoMedicamento;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsumoMedicamentoRepositoryPort {
  void guardar(ConsumoMedicamento consumo);

  Optional<ConsumoMedicamento> buscar(UUID id);

  Optional<ConsumoMedicamento> buscarPorIdempotencia(UUID diagnosticoId, UUID clave);

  List<ConsumoMedicamento> listarPorLote(UUID loteId);
}
