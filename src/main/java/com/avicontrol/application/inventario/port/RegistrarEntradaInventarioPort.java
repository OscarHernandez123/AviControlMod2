package com.avicontrol.application.inventario.port;

import com.avicontrol.domain.model.catalogo.UnidadMedidaMedicamento;
import java.math.BigDecimal;
import java.util.UUID;

public interface RegistrarEntradaInventarioPort {
  void alimento(UUID recepcionId, UUID alimentoId, BigDecimal kg, String actor);

  void medicamento(
      UUID recepcionId,
      UUID medicamentoId,
      BigDecimal cantidad,
      UnidadMedidaMedicamento unidadBase,
      String actor);
}
