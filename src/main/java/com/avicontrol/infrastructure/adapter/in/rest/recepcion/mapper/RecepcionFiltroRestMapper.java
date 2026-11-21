package com.avicontrol.infrastructure.adapter.in.rest.recepcion.mapper;

import com.avicontrol.domain.exception.recepcion.CantidadInvalidaException;
import com.avicontrol.domain.model.recepcion.EstadoRecepcion;
import com.avicontrol.domain.model.recepcion.FiltroRecepciones;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class RecepcionFiltroRestMapper {
  private final Clock clock;

  public RecepcionFiltroRestMapper(Clock clock) {
    this.clock = clock;
  }

  public FiltroRecepciones crear(
      String busqueda,
      EstadoRecepcion estado,
      LocalDate desde,
      LocalDate hasta,
      String presentacion,
      int pagina,
      int tamano) {
    LocalDate hoy = LocalDate.now(clock);
    LocalDate inicio = desde == null ? hoy.minusDays(29) : desde;
    LocalDate fin = hasta == null ? hoy : hasta;
    if (pagina < 0 || tamano < 1 || tamano > 100 || inicio.isAfter(fin)) {
      throw new CantidadInvalidaException("Paginación o periodo inválido");
    }
    return new FiltroRecepciones(busqueda, estado, inicio, fin, presentacion, pagina, tamano);
  }
}
