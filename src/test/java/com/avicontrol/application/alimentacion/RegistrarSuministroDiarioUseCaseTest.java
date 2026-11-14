package com.avicontrol.application.alimentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.avicontrol.domain.exception.alimentacion.AlimentoNoCorrespondeEtapaException;
import com.avicontrol.domain.exception.alimentacion.SuministroDuplicadoException;
import com.avicontrol.domain.model.alimentacion.SuministroDiarioAlimento;
import com.avicontrol.domain.port.out.alimentacion.InventarioAlimentoPort;
import com.avicontrol.domain.port.out.alimentacion.PlanNutricionalQueryPort;
import com.avicontrol.domain.port.out.alimentacion.SuministroDiarioRepositoryPort;
import com.avicontrol.domain.port.out.sanitario.medicacion.GalponLoteQueryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RegistrarSuministroDiarioUseCaseTest {
  private final LocalDate hoy = LocalDate.of(2026, 11, 6);
  private final Clock clock =
      Clock.fixed(hoy.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
  private final GalponLoteQueryPort galpones = Mockito.mock(GalponLoteQueryPort.class);
  private final PlanNutricionalQueryPort planes = Mockito.mock(PlanNutricionalQueryPort.class);
  private final InventarioAlimentoPort inventario = Mockito.mock(InventarioAlimentoPort.class);
  private final SuministroDiarioRepositoryPort repo =
      Mockito.mock(SuministroDiarioRepositoryPort.class);
  private final RegistrarSuministroDiarioUseCase useCase =
      new RegistrarSuministroDiarioUseCase(galpones, planes, inventario, repo, clock);

  @Test
  void sinPlanNoSolicitaSalidaNiPersiste() {
    UUID galpon = UUID.randomUUID();
    UUID lote = UUID.randomUUID();
    UUID clave = UUID.randomUUID();
    when(galpones.bloquear(galpon))
        .thenReturn(
            Optional.of(
                new GalponLoteQueryPort.GalponLote(
                    galpon, "G", 100, "PRODUCTIVA", lote, "L", 80, hoy.minusDays(5))));
    when(planes.buscar(lote, hoy, 6)).thenReturn(Optional.empty());
    assertThrows(
        AlimentoNoCorrespondeEtapaException.class,
        () -> useCase.registrar(galpon, BigDecimal.TEN, hoy, clave, "worker"));
    verifyNoInteractions(inventario);
  }

  @Test
  void reintentoIgualDevuelveOriginalYDiferenteDaConflicto() {
    UUID galpon = UUID.randomUUID();
    UUID lote = UUID.randomUUID();
    UUID alimento = UUID.randomUUID();
    UUID clave = UUID.randomUUID();
    var original =
        new SuministroDiarioAlimento(
            UUID.randomUUID(),
            galpon,
            lote,
            alimento,
            "INICIO",
            BigDecimal.TEN,
            hoy,
            Instant.now(clock),
            "worker",
            clave,
            List.of(
                new SuministroDiarioAlimento.Detalle(
                    UUID.randomUUID(), BigDecimal.TEN, BigDecimal.ONE)));
    when(repo.buscarPorClave(galpon, clave)).thenReturn(Optional.of(original));
    var resultado = useCase.registrar(galpon, BigDecimal.TEN, hoy, clave, "worker");
    assertEquals(true, resultado.repetido());
    assertEquals(original.id(), resultado.suministro().id());
    assertThrows(
        SuministroDuplicadoException.class,
        () -> useCase.registrar(galpon, BigDecimal.ONE, hoy, clave, "worker"));
    verifyNoInteractions(galpones, planes, inventario);
  }
}
