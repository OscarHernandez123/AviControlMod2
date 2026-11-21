package com.avicontrol.domain.model.sacrificio.productivo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.avicontrol.domain.exception.sacrificio.productivo.ResultadoSacrificioNoModificableException;
import com.avicontrol.domain.exception.sacrificio.productivo.ValoresResultadoSacrificioInvalidosException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResultadoSacrificioProductivoTest {
  @Test
  void validaValoresYBloqueaCorreccionUtilizada() {
    var orden =
        new OrdenSacrificioProductivo(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            EstadoOrdenSacrificio.EJECUTADA,
            Instant.now(),
            Instant.now(),
            null,
            null,
            Instant.now(),
            1);
    assertThrows(
        ValoresResultadoSacrificioInvalidosException.class,
        () ->
            ResultadoSacrificioProductivo.registrar(
                orden, 0, new BigDecimal("1.00"), Instant.now()));
    assertThrows(
        ValoresResultadoSacrificioInvalidosException.class,
        () ->
            ResultadoSacrificioProductivo.registrar(
                orden, 1, new BigDecimal("1.001"), Instant.now()));
    var resultado =
        ResultadoSacrificioProductivo.registrar(
            orden, 7800, new BigDecimal("18720.50"), Instant.now());
    assertEquals(1, resultado.corregir(7790, new BigDecimal("18700.00"), Instant.now()).version());
    var usado =
        new ResultadoSacrificioProductivo(
            resultado.id(),
            resultado.ordenId(),
            resultado.galponId(),
            resultado.loteId(),
            resultado.cantidadFinalPollos(),
            resultado.pesoTotalKg(),
            EstadoResultadoSacrificio.UTILIZADO,
            resultado.fechaRegistro(),
            null,
            1);
    assertThrows(
        ResultadoSacrificioNoModificableException.class,
        () -> usado.corregir(1, BigDecimal.ONE, Instant.now()));
  }
}
