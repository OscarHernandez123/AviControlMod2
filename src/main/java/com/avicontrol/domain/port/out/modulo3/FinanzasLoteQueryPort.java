package com.avicontrol.domain.port.out.modulo3;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface FinanzasLoteQueryPort {
  List<PartidaAlimento> alimentos(UUID loteId);

  List<PartidaMedicamento> medicamentos(UUID loteId);

  record PartidaAlimento(
      UUID suministroId,
      UUID recepcionId,
      UUID galponId,
      UUID alimentoId,
      String nombreAlimento,
      String tipoAlimento,
      LocalDate fechaAplicacion,
      BigDecimal cantidadKg,
      BigDecimal precioNetoKg,
      BigDecimal impuestoPorcentaje,
      BigDecimal valorImpuestoCop,
      BigDecimal valorImpuestoRecepcionCop,
      String moneda,
      boolean datosCompletos) {}

  record PartidaMedicamento(
      UUID consumoId,
      UUID recepcionId,
      UUID galponId,
      UUID medicamentoId,
      String nombreMedicamento,
      LocalDate fechaAplicacion,
      BigDecimal cantidadUnidadBase,
      String unidadBase,
      BigDecimal precioNetoHistoricoUnidadBase,
      BigDecimal impuestoPorcentaje,
      BigDecimal valorImpuestoCop,
      BigDecimal valorImpuestoRecepcionCop,
      String moneda,
      boolean datosCompletos) {}
}
