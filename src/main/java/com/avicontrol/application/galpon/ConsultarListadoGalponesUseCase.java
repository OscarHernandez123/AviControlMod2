package com.avicontrol.application.galpon;

import com.avicontrol.application.galpon.result.GalponListadoResult;
import com.avicontrol.application.galpon.result.ListadoGalponesResult;
import com.avicontrol.application.support.CurrentActorProvider;
import com.avicontrol.domain.exception.BusinessRuleViolationException;
import com.avicontrol.domain.port.out.galpon.GalponPage;
import com.avicontrol.domain.port.out.galpon.GalponPageQuery;
import com.avicontrol.domain.port.out.galpon.GalponQueryPort;
import java.util.List;

public final class ConsultarListadoGalponesUseCase {

  private static final int MAX_PAGE_SIZE = 100;
  private final GalponQueryPort galponQueryPort;
  private final CurrentActorProvider currentActorProvider;

  public ConsultarListadoGalponesUseCase(
      GalponQueryPort galponQueryPort, CurrentActorProvider currentActorProvider) {
    this.galponQueryPort = galponQueryPort;
    this.currentActorProvider = currentActorProvider;
  }

  public ListadoGalponesResult execute(int page, int size, String sort) {
    GalponAuthorization.requireReader(currentActorProvider.currentActor());
    if (page < 0 || size <= 0 || size > MAX_PAGE_SIZE) {
      throw new BusinessRuleViolationException(
          "PAGINACION_INVALIDA", "La paginación solicitada no es válida");
    }
    String[] sortParts = sort.split(",", -1);
    if (sortParts.length != 2
        || !(sortParts[0].equals("nombre") || sortParts[0].equals("estado"))) {
      throw new BusinessRuleViolationException(
          "ORDEN_INVALIDO", "El criterio de orden no es válido");
    }
    GalponPageQuery.SortDirection direction;
    try {
      direction = GalponPageQuery.SortDirection.valueOf(sortParts[1].toUpperCase());
    } catch (IllegalArgumentException exception) {
      throw new BusinessRuleViolationException(
          "ORDEN_INVALIDO", "El criterio de orden no es válido");
    }
    GalponPage result =
        galponQueryPort.listar(
            new GalponPageQuery(Math.multiplyExact(page, size), size, sortParts[0], direction));
    List<GalponListadoResult> content =
        result.content().stream()
            .map(g -> new GalponListadoResult(g.id(), g.nombre(), g.estado()))
            .toList();
    int totalPages =
        result.totalElements() == 0
            ? 0
            : Math.toIntExact((result.totalElements() + size - 1) / size);
    return new ListadoGalponesResult(
        content,
        page,
        size,
        result.totalElements(),
        totalPages,
        page == 0,
        result.totalElements() == 0 || page + 1 >= totalPages,
        content.isEmpty() && result.totalElements() == 0
            ? "No existen galpones disponibles"
            : null);
  }
}
