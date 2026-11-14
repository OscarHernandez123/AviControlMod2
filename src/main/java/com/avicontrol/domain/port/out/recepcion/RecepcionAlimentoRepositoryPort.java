package com.avicontrol.domain.port.out.recepcion;

import com.avicontrol.domain.model.recepcion.FiltroRecepciones;
import com.avicontrol.domain.model.recepcion.PaginaRecepciones;
import com.avicontrol.domain.model.recepcion.RecepcionAlimento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface RecepcionAlimentoRepositoryPort {
  void guardar(RecepcionAlimento recepcion);

  Optional<RecepcionAlimento> buscar(UUID id);

  Optional<RecepcionAlimento> bloquear(UUID id);

  void actualizar(RecepcionAlimento recepcion);

  PaginaRecepciones<RecepcionAlimento> listar(FiltroRecepciones filtro);

  long contarMes(LocalDate desde, LocalDate hasta);

  BigDecimal pesoMes(LocalDate desde, LocalDate hasta);
}
