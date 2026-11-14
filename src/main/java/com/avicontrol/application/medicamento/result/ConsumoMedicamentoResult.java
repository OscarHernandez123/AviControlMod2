package com.avicontrol.application.medicamento.result;

import com.avicontrol.domain.model.medicamento.ConsumoMedicamento;
import com.avicontrol.domain.model.medicamento.DetalleConsumoMedicamento;
import com.avicontrol.domain.model.medicamento.UnidadConsumoMedicamento;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ConsumoMedicamentoResult(
    UUID id,
    UUID diagnosticoId,
    UUID galponId,
    UUID loteId,
    UUID medicamentoId,
    BigDecimal cantidadIngresada,
    UnidadConsumoMedicamento unidadIngresada,
    BigDecimal cantidadUnidadBase,
    UnidadConsumoMedicamento unidadBase,
    LocalDate fechaAplicacion,
    Instant fechaRegistro,
    String actor,
    List<DetalleConsumoMedicamento> detallesInventario) {
  public static ConsumoMedicamentoResult de(ConsumoMedicamento c) {
    return new ConsumoMedicamentoResult(
        c.id(),
        c.diagnosticoId(),
        c.galponId(),
        c.loteId(),
        c.medicamentoId(),
        c.cantidadIngresada(),
        c.unidadIngresada(),
        c.cantidadUnidadBase(),
        c.unidadBase(),
        c.fechaAplicacion(),
        c.fechaRegistro(),
        c.actor(),
        c.detallesInventario());
  }
}
