package com.avicontrol.application.alimentacion;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.avicontrol.application.alimentacion.port.ProyeccionAlimentoRepositoryPort;
import com.avicontrol.domain.model.catalogo.EtapaCrianza;
import com.avicontrol.domain.model.nutricion.PlanNutricionalGalpon;
import com.avicontrol.domain.port.out.nutricion.PlanNutricionalGalponRepositoryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProyeccionAlimentoUseCaseTest {
  @Mock ProyeccionAlimentoRepositoryPort repositorio;
  @Mock PlanNutricionalGalponRepositoryPort planes;

  private final Clock clock =
      Clock.fixed(Instant.parse("2026-11-20T15:00:00Z"), ZoneId.of("America/Bogota"));

  @Test
  void activaProyeccionCalculandoKgTotalesYCostoPonderado() {
    ProyeccionAlimentoUseCase useCase = new ProyeccionAlimentoUseCase(repositorio, planes, clock);
    UUID alimentoId = UUID.randomUUID();
    UUID etapaId = UUID.randomUUID();
    PlanNutricionalGalpon.Etapa etapa =
        new PlanNutricionalGalpon.Etapa(
            etapaId,
            EtapaCrianza.INICIO,
            "ACTIVA",
            8,
            17,
            new BigDecimal("0.080"),
            EtapaCrianza.INICIO,
            alimentoId,
            new BigDecimal("40"));
    PlanNutricionalGalpon plan =
        new PlanNutricionalGalpon(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "L-01",
            UUID.randomUUID(),
            "ROSS_308",
            LocalDate.of(2026, 11, 10),
            1,
            500,
            LocalDate.of(2026, 12, 22),
            "ACTIVO",
            List.of(etapa));

    when(repositorio.existe(etapaId)).thenReturn(false);
    when(repositorio.saldosElegibles(alimentoId, LocalDate.now(clock)))
        .thenReturn(
            List.of(
                new ProyeccionAlimentoRepositoryPort.CostoSaldo(
                    new BigDecimal("2500"), new BigDecimal("1000")),
                new ProyeccionAlimentoRepositoryPort.CostoSaldo(
                    new BigDecimal("3000"), new BigDecimal("1000"))));

    useCase.activar(plan, etapa);

    verify(repositorio)
        .crear(
            argThat(
                p ->
                    p.proyeccionKg().compareTo(new BigDecimal("400.000")) == 0
                        && p.costoUnitarioKg().compareTo(new BigDecimal("2750.0000")) == 0
                        && "ACTIVA".equals(p.estado())));
  }

  @Test
  void noDuplicaProyeccionSiEtapaYaExiste() {
    ProyeccionAlimentoUseCase useCase = new ProyeccionAlimentoUseCase(repositorio, planes, clock);
    UUID etapaId = UUID.randomUUID();
    PlanNutricionalGalpon.Etapa etapa =
        new PlanNutricionalGalpon.Etapa(
            etapaId,
            EtapaCrianza.INICIO,
            "ACTIVA",
            8,
            17,
            new BigDecimal("0.080"),
            EtapaCrianza.INICIO,
            UUID.randomUUID(),
            new BigDecimal("40"));
    PlanNutricionalGalpon plan =
        new PlanNutricionalGalpon(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "L-01",
            UUID.randomUUID(),
            "ROSS_308",
            LocalDate.of(2026, 11, 10),
            1,
            500,
            LocalDate.of(2026, 12, 22),
            "ACTIVO",
            List.of(etapa));

    when(repositorio.existe(etapaId)).thenReturn(true);

    useCase.activar(plan, etapa);

    verify(repositorio, never()).crear(org.mockito.ArgumentMatchers.any());
  }
}
