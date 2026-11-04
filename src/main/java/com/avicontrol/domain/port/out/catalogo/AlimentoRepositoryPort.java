package com.avicontrol.domain.port.out.catalogo;

import com.avicontrol.domain.model.catalogo.Alimento;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlimentoRepositoryPort {
  Alimento guardar(Alimento alimento);

  Optional<Alimento> buscarPorId(UUID id);

  List<Alimento> listar();
}
