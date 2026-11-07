package com.avicontrol.domain.model.sacrificio.productivo;

import com.avicontrol.domain.exception.sacrificio.productivo.ResultadoSacrificioNoModificableException;
import com.avicontrol.domain.exception.sacrificio.productivo.ValoresResultadoSacrificioInvalidosException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ResultadoSacrificioProductivo(
    UUID id,
    UUID ordenId,
    UUID galponId,
    UUID loteId,
    int cantidadFinalPollos,
    BigDecimal pesoTotalKg,
    EstadoResultadoSacrificio estado,
    Instant fechaRegistro,
    Instant fechaCorreccion,
    long version) {
  public static ResultadoSacrificioProductivo registrar(
      OrdenSacrificioProductivo orden, int cantidad, BigDecimal peso, Instant ahora) {
    validar(cantidad, peso);
    return new ResultadoSacrificioProductivo(
        UUID.randomUUID(),
        orden.id(),
        orden.galponId(),
        orden.loteId(),
        cantidad,
        peso,
        EstadoResultadoSacrificio.DISPONIBLE,
        ahora,
        null,
        0);
  }

  public ResultadoSacrificioProductivo corregir(int cantidad, BigDecimal peso, Instant ahora) {
    if (estado != EstadoResultadoSacrificio.DISPONIBLE)
      throw new ResultadoSacrificioNoModificableException();
    validar(cantidad, peso);
    return new ResultadoSacrificioProductivo(
        id, ordenId, galponId, loteId, cantidad, peso, estado, fechaRegistro, ahora, version + 1);
  }

  private static void validar(int cantidad, BigDecimal peso) {
    if (cantidad <= 0 || peso == null || peso.signum() <= 0 || peso.scale() > 2)
      throw new ValoresResultadoSacrificioInvalidosException();
  }
}
