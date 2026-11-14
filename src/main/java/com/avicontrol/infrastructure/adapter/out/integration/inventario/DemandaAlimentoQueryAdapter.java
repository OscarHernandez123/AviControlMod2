package com.avicontrol.infrastructure.adapter.out.integration.inventario;

import com.avicontrol.application.alimentacion.ProyeccionAlimentoUseCase;
import com.avicontrol.domain.port.out.inventario.DemandaAlimentoQueryPort;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DemandaAlimentoQueryAdapter implements DemandaAlimentoQueryPort {
  private final ProyeccionAlimentoUseCase proyecciones;

  public DemandaAlimentoQueryAdapter(ProyeccionAlimentoUseCase proyecciones) {
    this.proyecciones = proyecciones;
  }

  @Override
  public BigDecimal demandaVigente(UUID alimentoId, String nombreNormalizado, String tipoAlimento) {
    return proyecciones.demandaVigente(alimentoId, nombreNormalizado, tipoAlimento);
  }
}
