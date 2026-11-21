package com.avicontrol.application.inventario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.avicontrol.domain.model.inventario.ExistenciaAlimento;
import com.avicontrol.domain.model.inventario.InventarioActual;
import com.avicontrol.domain.port.out.inventario.DemandaAlimentoQueryPort;
import com.avicontrol.domain.port.out.inventario.InventarioMovimientoQueryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsultarInventarioUseCaseTest {
  @Mock InventarioMovimientoQueryPort movimientos;
  @Mock DemandaAlimentoQueryPort demanda;

  private final Clock clock =
      Clock.fixed(Instant.parse("2026-11-20T15:00:00Z"), ZoneId.of("America/Bogota"));

  @Test
  void clasificaEstadosCoberturaSegunStockYDemanda() {
    ConsultarInventarioUseCase useCase =
        new ConsultarInventarioUseCase(movimientos, demanda, clock);
    UUID idCumple = UUID.randomUUID();
    UUID idParcial = UUID.randomUUID();
    UUID idSinCob = UUID.randomUUID();
    UUID idSinReq = UUID.randomUUID();

    LocalDate hoy = LocalDate.now(clock);
    InventarioActual base =
        new InventarioActual(
            List.of(
                new ExistenciaAlimento(
                    idCumple,
                    "Inicio",
                    "INICIO",
                    1,
                    hoy.plusDays(20),
                    new BigDecimal("200"),
                    BigDecimal.ZERO,
                    ""),
                new ExistenciaAlimento(
                    idParcial,
                    "Engorde",
                    "ENGORDE",
                    1,
                    hoy.plusDays(15),
                    new BigDecimal("50"),
                    BigDecimal.ZERO,
                    ""),
                new ExistenciaAlimento(
                    idSinCob,
                    "Finalizador",
                    "FINALIZACION",
                    0,
                    null,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    ""),
                new ExistenciaAlimento(
                    idSinReq,
                    "Preiniciador",
                    "PREINICIO",
                    1,
                    hoy.plusDays(30),
                    new BigDecimal("100"),
                    BigDecimal.ZERO,
                    "")),
            List.of(),
            Map.of("TOTAL", new BigDecimal("350")),
            Map.of());

    when(movimientos.consultar(hoy)).thenReturn(base);
    when(demanda.demandaVigente(idCumple, "Inicio", "INICIO")).thenReturn(new BigDecimal("150"));
    when(demanda.demandaVigente(idParcial, "Engorde", "ENGORDE")).thenReturn(new BigDecimal("120"));
    when(demanda.demandaVigente(idSinCob, "Finalizador", "FINALIZACION"))
        .thenReturn(new BigDecimal("80"));
    when(demanda.demandaVigente(idSinReq, "Preiniciador", "PREINICIO")).thenReturn(BigDecimal.ZERO);

    InventarioActual resultado = useCase.consultar();

    assertThat(resultado.alimentos())
        .extracting(ExistenciaAlimento::estadoCobertura)
        .containsExactly("CUMPLE", "COBERTURA_PARCIAL", "SIN_COBERTURA", "SIN_REQUERIMIENTO");
  }
}
