package com.avicontrol.application.nutricion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.avicontrol.application.alimentacion.ProyeccionAlimentoUseCase;
import com.avicontrol.domain.model.catalogo.EtapaCrianza;
import com.avicontrol.domain.model.nutricion.PlanNutricionalGalpon;
import com.avicontrol.domain.model.nutricion.PlanNutricionalPlantilla;
import com.avicontrol.domain.port.out.nutricion.AlimentoCompatibleQueryPort;
import com.avicontrol.domain.port.out.nutricion.PlanNutricionalGalponRepositoryPort;
import com.avicontrol.domain.port.out.nutricion.PlanNutricionalPlantillaRepositoryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PlanNutricionalLoteUseCaseTest {
  @Mock PlanNutricionalGalponRepositoryPort planes;
  @Mock PlanNutricionalPlantillaRepositoryPort plantillas;
  @Mock AlimentoCompatibleQueryPort alimentos;
  @Mock ProyeccionAlimentoUseCase proyecciones;
  @Mock ApplicationEventPublisher eventos;

  private final Clock clock =
      Clock.fixed(Instant.parse("2026-11-20T15:00:00Z"), ZoneId.of("America/Bogota"));

  private PlanNutricionalPlantilla plantilla(UUID id, String tipoAve) {
    return new PlanNutricionalPlantilla(
        id,
        "Plantilla",
        tipoAve,
        true,
        true,
        Instant.now(clock),
        List.of(
            new PlanNutricionalPlantilla.Etapa(
                EtapaCrianza.PRE_INICIO,
                1,
                7,
                new BigDecimal("0.025"),
                EtapaCrianza.PRE_INICIO,
                new BigDecimal("40"),
                null),
            new PlanNutricionalPlantilla.Etapa(
                EtapaCrianza.INICIO,
                8,
                21,
                new BigDecimal("0.085"),
                EtapaCrianza.INICIO,
                new BigDecimal("40"),
                null),
            new PlanNutricionalPlantilla.Etapa(
                EtapaCrianza.ENGORDE,
                22,
                42,
                new BigDecimal("0.150"),
                EtapaCrianza.ENGORDE,
                new BigDecimal("40"),
                null)));
  }

  @Test
  void asignaPlanPendienteYPublicaEventoCuandoNoHayPlantillaPredeterminada() {
    PlanNutricionalLoteUseCase useCase =
        new PlanNutricionalLoteUseCase(planes, plantillas, alimentos, clock, proyecciones, eventos);
    UUID galponId = UUID.randomUUID();
    UUID loteId = UUID.randomUUID();
    var alojamiento =
        new PlanNutricionalLoteUseCase.Alojamiento(
            galponId, loteId, "L-01", "ROSS_308", LocalDate.of(2026, 11, 20), 1, 1000);

    when(planes.buscarPorLote(loteId)).thenReturn(Optional.empty());
    when(plantillas.predeterminada("ROSS_308")).thenReturn(Optional.empty());

    PlanNutricionalGalpon resultado = useCase.asignarAlAlojar(alojamiento);

    assertThat(resultado.estado()).isEqualTo("PENDIENTE_ASIGNACION");
    verify(planes).crear(any(PlanNutricionalGalpon.class));
    verify(eventos)
        .publishEvent(any(PlanNutricionalLoteUseCase.PlanNutricionalPendienteEvent.class));
  }

  @Test
  void asignaConPlantillaPredeterminadaYActivaEtapaActual() {
    PlanNutricionalLoteUseCase useCase =
        new PlanNutricionalLoteUseCase(planes, plantillas, alimentos, clock, proyecciones, eventos);
    UUID galponId = UUID.randomUUID();
    UUID loteId = UUID.randomUUID();
    var alojamiento =
        new PlanNutricionalLoteUseCase.Alojamiento(
            galponId, loteId, "L-02", "ROSS_308", LocalDate.of(2026, 11, 20), 1, 1000);
    PlanNutricionalPlantilla plantilla = plantilla(UUID.randomUUID(), "ROSS_308");

    when(planes.buscarPorLote(loteId)).thenReturn(Optional.empty());
    when(plantillas.predeterminada("ROSS_308")).thenReturn(Optional.of(plantilla));

    PlanNutricionalGalpon resultado = useCase.asignarAlAlojar(alojamiento);

    assertThat(resultado.estado()).isEqualTo("ACTIVO");
    assertThat(resultado.etapas()).hasSize(3);
    verify(proyecciones).activar(any(PlanNutricionalGalpon.class), any());
  }

  @Test
  void sustituyeSoloEtapasFuturasPreservandoCompletadasYActivas() {
    PlanNutricionalLoteUseCase useCase =
        new PlanNutricionalLoteUseCase(planes, plantillas, alimentos, clock, proyecciones, eventos);
    UUID galponId = UUID.randomUUID();
    UUID loteId = UUID.randomUUID();
    UUID nuevaPlantillaId = UUID.randomUUID();
    PlanNutricionalGalpon actual =
        new PlanNutricionalGalpon(
            UUID.randomUUID(),
            galponId,
            loteId,
            "L-04",
            UUID.randomUUID(),
            "ROSS_308",
            LocalDate.of(2026, 11, 1),
            1,
            1000,
            LocalDate.of(2026, 12, 12),
            "ACTIVO",
            List.of(
                new PlanNutricionalGalpon.Etapa(
                    UUID.randomUUID(),
                    EtapaCrianza.PRE_INICIO,
                    "COMPLETADA",
                    1,
                    7,
                    new BigDecimal("0.025"),
                    EtapaCrianza.PRE_INICIO,
                    null,
                    new BigDecimal("40")),
                new PlanNutricionalGalpon.Etapa(
                    UUID.randomUUID(),
                    EtapaCrianza.INICIO,
                    "ACTIVA",
                    8,
                    21,
                    new BigDecimal("0.085"),
                    EtapaCrianza.INICIO,
                    null,
                    new BigDecimal("40")),
                new PlanNutricionalGalpon.Etapa(
                    UUID.randomUUID(),
                    EtapaCrianza.ENGORDE,
                    "FUTURA",
                    22,
                    42,
                    new BigDecimal("0.150"),
                    EtapaCrianza.ENGORDE,
                    null,
                    new BigDecimal("40"))));

    PlanNutricionalPlantilla nueva =
        new PlanNutricionalPlantilla(
            nuevaPlantillaId,
            "Nueva",
            "ROSS_308",
            false,
            true,
            Instant.now(clock),
            List.of(
                new PlanNutricionalPlantilla.Etapa(
                    EtapaCrianza.PRE_INICIO,
                    1,
                    7,
                    new BigDecimal("0.030"),
                    EtapaCrianza.PRE_INICIO,
                    new BigDecimal("40"),
                    null),
                new PlanNutricionalPlantilla.Etapa(
                    EtapaCrianza.INICIO,
                    8,
                    21,
                    new BigDecimal("0.090"),
                    EtapaCrianza.INICIO,
                    new BigDecimal("40"),
                    null),
                new PlanNutricionalPlantilla.Etapa(
                    EtapaCrianza.ENGORDE,
                    22,
                    45,
                    new BigDecimal("0.165"),
                    EtapaCrianza.ENGORDE,
                    new BigDecimal("40"),
                    null)));

    when(planes.buscarPorGalpon(galponId)).thenReturn(Optional.of(actual));
    when(plantillas.buscar(nuevaPlantillaId)).thenReturn(Optional.of(nueva));

    PlanNutricionalGalpon resultado =
        useCase.sustituir(galponId, nuevaPlantillaId, "Ajuste nutricional");

    assertThat(resultado.etapas().get(0).racionKgAveDia()).isEqualByComparingTo("0.025");
    assertThat(resultado.etapas().get(1).racionKgAveDia()).isEqualByComparingTo("0.085");
    assertThat(resultado.etapas().get(2).racionKgAveDia()).isEqualByComparingTo("0.165");
    assertThat(resultado.etapas().get(2).diaFin()).isEqualTo(45);
    verify(planes).actualizar(any(PlanNutricionalGalpon.class));
  }

  @Test
  void rechazaAlojamientoInvalido() {
    PlanNutricionalLoteUseCase useCase =
        new PlanNutricionalLoteUseCase(planes, plantillas, alimentos, clock, proyecciones, eventos);
    var invalido =
        new PlanNutricionalLoteUseCase.Alojamiento(
            UUID.randomUUID(), UUID.randomUUID(), "L-03", "ROSS_308", LocalDate.now(), 0, 100);

    assertThatThrownBy(() -> useCase.asignarAlAlojar(invalido))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
