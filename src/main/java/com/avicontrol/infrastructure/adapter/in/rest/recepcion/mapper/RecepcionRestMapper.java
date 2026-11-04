package com.avicontrol.infrastructure.adapter.in.rest.recepcion.mapper;

import com.avicontrol.application.recepcion.PaginaResultado;
import java.util.List;
import org.springframework.stereotype.Component;

/** Shared mapping boundary for REST response DTOs. */
@Component
public class RecepcionRestMapper {
  public <T> PaginaResultado<T> pagina(
      List<T> content, int page, int size, long totalElements, int totalPages) {
    return new PaginaResultado<>(content, page, size, totalElements, totalPages);
  }
}
