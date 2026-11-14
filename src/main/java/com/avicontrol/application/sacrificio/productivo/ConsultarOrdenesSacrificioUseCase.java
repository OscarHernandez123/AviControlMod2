package com.avicontrol.application.sacrificio.productivo;

import com.avicontrol.application.sacrificio.productivo.result.OrdenSacrificioResult;
import com.avicontrol.domain.port.out.sacrificio.productivo.OrdenSacrificioProductivoRepositoryPort;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

public class ConsultarOrdenesSacrificioUseCase {
  private final OrdenSacrificioProductivoRepositoryPort ordenes;

  public ConsultarOrdenesSacrificioUseCase(OrdenSacrificioProductivoRepositoryPort ordenes) {
    this.ordenes = ordenes;
  }

  @Transactional(readOnly = true)
  public List<OrdenSacrificioResult> listar() {
    return ordenes.listar().stream().map(OrdenSacrificioResult::desde).toList();
  }
}
