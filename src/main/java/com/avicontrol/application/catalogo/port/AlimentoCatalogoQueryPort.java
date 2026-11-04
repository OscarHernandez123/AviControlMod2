package com.avicontrol.application.catalogo.port;

import com.avicontrol.domain.model.catalogo.Alimento;
import java.util.Optional;
import java.util.UUID;

public interface AlimentoCatalogoQueryPort {
  Optional<Alimento> buscarAlimentoActivo(UUID alimentoId);
}
