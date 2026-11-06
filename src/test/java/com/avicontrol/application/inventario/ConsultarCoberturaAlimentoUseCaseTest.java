package com.avicontrol.application.inventario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.avicontrol.domain.model.inventario.ExistenciaAlimento;
import com.avicontrol.domain.model.inventario.InventarioActual;
import com.avicontrol.domain.port.out.inventario.DemandaAlimentoQueryPort;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsultarCoberturaAlimentoUseCaseTest {
  private static final UUID ALIMENTO = UUID.randomUUID();

  @Test
  void clasificaLosCuatroEstadosSinAlterarInventario() {
    verificar("100", "0", "SIN_REQUERIMIENTO");
    verificar("0", "50", "SIN_COBERTURA");
    verificar("30", "50", "COBERTURA_PARCIAL");
    verificar("50", "50", "CUMPLE");
  }

  private void verificar(String stock, String demandaKg, String esperado) {
    ConsultarInventarioUseCase inventario = mock(ConsultarInventarioUseCase.class);
    DemandaAlimentoQueryPort demanda = mock(DemandaAlimentoQueryPort.class);
    when(inventario.consultar())
        .thenReturn(
            new InventarioActual(
                List.of(
                    new ExistenciaAlimento(
                        ALIMENTO,
                        "Inicio Broiler",
                        "INICIO",
                        1,
                        null,
                        new BigDecimal(stock),
                        BigDecimal.ZERO,
                        "SIN_REQUERIMIENTO")),
                List.of(),
                Map.of()));
    when(demanda.demandaVigente(ALIMENTO, "Inicio Broiler", "INICIO"))
        .thenReturn(new BigDecimal(demandaKg));
    var resultado = new ConsultarCoberturaAlimentoUseCase(inventario, demanda).consultar(ALIMENTO);
    assertEquals(esperado, resultado.estado());
  }
}
