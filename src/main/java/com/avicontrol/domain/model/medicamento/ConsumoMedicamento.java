package com.avicontrol.domain.model.medicamento;

import com.avicontrol.domain.exception.medicamento.CantidadMedicamentoInvalidaException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ConsumoMedicamento(
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
    UUID idempotenciaClave,
    List<DetalleConsumoMedicamento> detallesInventario) {
  public ConsumoMedicamento {
    if (id == null
        || diagnosticoId == null
        || galponId == null
        || loteId == null
        || medicamentoId == null
        || unidadIngresada == null
        || unidadBase == null
        || fechaAplicacion == null
        || fechaRegistro == null
        || idempotenciaClave == null
        || actor == null
        || actor.isBlank()
        || detallesInventario == null
        || detallesInventario.isEmpty())
      throw new CantidadMedicamentoInvalidaException("Datos obligatorios del consumo incompletos");
    if (unidadIngresada.unidadBase() != unidadBase
        || cantidadIngresada == null
        || cantidadUnidadBase == null
        || cantidadUnidadBase.compareTo(unidadIngresada.normalizar(cantidadIngresada)) != 0)
      throw new CantidadMedicamentoInvalidaException("Cantidad o unidad base inconsistente");
    BigDecimal suma =
        detallesInventario.stream()
            .map(DetalleConsumoMedicamento::cantidadUnidadBase)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (suma.compareTo(cantidadUnidadBase) != 0)
      throw new CantidadMedicamentoInvalidaException(
          "La suma de detalles no coincide con el consumo");
    detallesInventario = List.copyOf(detallesInventario);
    actor = actor.strip();
  }
}
