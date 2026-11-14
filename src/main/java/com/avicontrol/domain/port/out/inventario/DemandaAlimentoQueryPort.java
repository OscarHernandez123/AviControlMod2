package com.avicontrol.domain.port.out.inventario;

import java.math.BigDecimal;
import java.util.UUID;

public interface DemandaAlimentoQueryPort {
  BigDecimal demandaVigente(UUID alimentoId, String nombreNormalizado, String tipoAlimento);
}
