package com.avicontrol.application.alimentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.avicontrol.application.alimentacion.result.AlimentoDiarioGalponResult;
import com.avicontrol.domain.model.alimentacion.RequerimientoDiarioGalpon;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ConsultarResumenAlimentoDiarioUseCaseTest {
  @Test
  void agrupaDemandaDeGalponesPorElMismoAlimento() {
    LocalDate hoy = LocalDate.of(2026, 11, 6);
    Clock clock = Clock.fixed(hoy.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    var detalle = Mockito.mock(ConsultarAlimentoGalponDiaUseCase.class);
    UUID alimento = UUID.randomUUID();
    UUID uno = UUID.randomUUID();
    UUID dos = UUID.randomUUID();
    when(detalle.consultar(uno, hoy))
        .thenReturn(fila(uno, alimento, hoy, new BigDecimal("450"), new BigDecimal("600")));
    when(detalle.consultar(dos, hoy))
        .thenReturn(fila(dos, alimento, hoy, new BigDecimal("430"), new BigDecimal("600")));
    var resultado =
        new ConsultarResumenAlimentoDiarioUseCase(detalle, clock)
            .consultar(List.of(uno, dos, uno), null);
    assertEquals(0, resultado.requerimientoTotalKg().compareTo(new BigDecimal("880")));
    assertEquals(2, resultado.galpones().size());
    assertEquals("BODEGA_INSUFICIENTE", resultado.estadoBodega());
    assertEquals("INSUFICIENTE", resultado.galpones().getFirst().estadoBodega());
  }

  private AlimentoDiarioGalponResult fila(
      UUID galpon, UUID alimento, LocalDate fecha, BigDecimal kg, BigDecimal disponible) {
    var q =
        new RequerimientoDiarioGalpon(
            BigDecimal.valueOf(120),
            kg,
            RequerimientoDiarioGalpon.bultos(kg, BigDecimal.valueOf(50)),
            BigDecimal.ZERO,
            kg,
            RequerimientoDiarioGalpon.bultos(kg, BigDecimal.valueOf(50)),
            disponible,
            RequerimientoDiarioGalpon.bultos(disponible, BigDecimal.valueOf(50)));
    return new AlimentoDiarioGalponResult(
        galpon,
        "Galpón",
        10000,
        "PRODUCTIVA",
        UUID.randomUUID(),
        "L-1",
        8000,
        5,
        fecha,
        "CON_PLAN_NUTRICIONAL",
        "INICIO",
        "INICIO",
        alimento,
        "Inicio",
        BigDecimal.valueOf(50),
        q,
        List.of());
  }
}
