package com.avicontrol.domain.port.out.recepcion;

import com.avicontrol.domain.model.recepcion.FiltroRecepciones;
import com.avicontrol.domain.model.recepcion.PaginaRecepciones;
import com.avicontrol.domain.model.recepcion.RecepcionMedicamento;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface RecepcionMedicamentoRepositoryPort {
  void guardar(RecepcionMedicamento recepcion);

  Optional<RecepcionMedicamento> buscar(UUID id);

  Optional<RecepcionMedicamento> bloquear(UUID id);

  void actualizar(RecepcionMedicamento recepcion);

  PaginaRecepciones<RecepcionMedicamento> listar(FiltroRecepciones filtro);

  long contarMes(LocalDate desde, LocalDate hasta);
}
