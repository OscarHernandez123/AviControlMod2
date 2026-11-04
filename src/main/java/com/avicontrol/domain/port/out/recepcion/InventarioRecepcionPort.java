package com.avicontrol.domain.port.out.recepcion;

import java.math.BigDecimal;
import java.util.UUID;

public interface InventarioRecepcionPort {
  void registrarAlimento(UUID recepcionId, BigDecimal cantidadKg);

  void registrarMedicamento(UUID recepcionId, BigDecimal cantidadUnidadBase, String unidadBase);

  void ajustarAlimento(UUID recepcionId, BigDecimal cantidadKg);

  void ajustarMedicamento(UUID recepcionId, BigDecimal cantidadUnidadBase, String unidadBase);

  boolean alimentoUtilizado(UUID id);

  boolean medicamentoUtilizado(UUID id);
}
