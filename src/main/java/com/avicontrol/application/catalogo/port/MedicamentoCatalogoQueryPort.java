package com.avicontrol.application.catalogo.port;

import com.avicontrol.domain.model.catalogo.Medicamento;
import java.util.Optional;
import java.util.UUID;

public interface MedicamentoCatalogoQueryPort {
  Optional<Medicamento> buscarMedicamentoActivo(UUID medicamentoId);
}
