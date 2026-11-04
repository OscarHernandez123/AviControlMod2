package com.avicontrol.domain.port.out.recepcion;

import com.avicontrol.domain.model.recepcion.RecepcionMedicamento;
import java.util.*;

public interface RecepcionMedicamentoRepositoryPort {
  RecepcionMedicamento guardar(RecepcionMedicamento r);

  Optional<RecepcionMedicamento> buscarMedicamentoPorId(UUID id);

  List<RecepcionMedicamento> listarMedicamentos();
}
