package com.avicontrol.application.sacrificio.productivo;

import com.avicontrol.application.sacrificio.productivo.result.ResumenOrdenesSacrificioResult;
import com.avicontrol.domain.port.out.sacrificio.productivo.OrdenSacrificioProductivoRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

public class ConsultarResumenOrdenesSacrificioUseCase {
  private final OrdenSacrificioProductivoRepositoryPort ordenes;

  public ConsultarResumenOrdenesSacrificioUseCase(OrdenSacrificioProductivoRepositoryPort ordenes) {
    this.ordenes = ordenes;
  }

  @Transactional(readOnly = true)
  public ResumenOrdenesSacrificioResult consultar() {
    var resumen = ordenes.resumen();
    return new ResumenOrdenesSacrificioResult(resumen.totalOrdenes(), resumen.ordenesPendientes());
  }
}
