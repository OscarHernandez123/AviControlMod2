package com.avicontrol.domain.port.out.medicamento;

import com.avicontrol.domain.model.medicamento.DetalleConsumoMedicamento;
import com.avicontrol.domain.model.medicamento.UnidadConsumoMedicamento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface InventarioMedicamentoPort {
  List<DetalleConsumoMedicamento> reservarFefo(
      UUID medicamentoId,
      BigDecimal cantidadUnidadBase,
      UnidadConsumoMedicamento unidadBase,
      LocalDate fechaCorte);
}
