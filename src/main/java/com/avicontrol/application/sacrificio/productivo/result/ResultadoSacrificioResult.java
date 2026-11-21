package com.avicontrol.application.sacrificio.productivo.result;

import com.avicontrol.domain.model.sacrificio.productivo.ResultadoSacrificioProductivo;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ResultadoSacrificioResult(
    UUID id,
    UUID ordenId,
    UUID galponId,
    UUID loteId,
    int cantidadFinalPollos,
    BigDecimal pesoTotalKg,
    String estado,
    Instant fechaRegistro,
    Instant fechaCorreccion) {
  public static ResultadoSacrificioResult desde(ResultadoSacrificioProductivo resultado) {
    return new ResultadoSacrificioResult(
        resultado.id(),
        resultado.ordenId(),
        resultado.galponId(),
        resultado.loteId(),
        resultado.cantidadFinalPollos(),
        resultado.pesoTotalKg(),
        resultado.estado().name(),
        resultado.fechaRegistro(),
        resultado.fechaCorreccion());
  }
}
