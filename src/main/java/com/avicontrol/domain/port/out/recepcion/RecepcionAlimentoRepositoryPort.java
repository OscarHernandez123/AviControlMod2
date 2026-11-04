package com.avicontrol.domain.port.out.recepcion;

import com.avicontrol.domain.model.recepcion.RecepcionAlimento;
import java.util.*;

public interface RecepcionAlimentoRepositoryPort {
  RecepcionAlimento guardar(RecepcionAlimento r);

  Optional<RecepcionAlimento> buscarPorId(UUID id);

  List<RecepcionAlimento> listar();
}
