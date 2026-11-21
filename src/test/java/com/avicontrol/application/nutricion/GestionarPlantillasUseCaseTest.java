package com.avicontrol.application.nutricion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.avicontrol.domain.model.catalogo.EtapaCrianza;
import com.avicontrol.domain.model.nutricion.PlanNutricionalPlantilla;
import com.avicontrol.domain.port.out.nutricion.AlimentoCompatibleQueryPort;
import com.avicontrol.domain.port.out.nutricion.PlanNutricionalPlantillaRepositoryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GestionarPlantillasUseCaseTest {
  @Mock PlanNutricionalPlantillaRepositoryPort repositorio;
  @Mock AlimentoCompatibleQueryPort alimentos;

  private final Clock clock =
      Clock.fixed(Instant.parse("2026-11-20T15:00:00Z"), ZoneId.of("America/Bogota"));

  private PlanNutricionalPlantilla plantillaValida(String tipoAve) {
    return new PlanNutricionalPlantilla(
        UUID.randomUUID(),
        "Ross 308 Estándar",
        tipoAve,
        true,
        true,
        Instant.now(clock),
        List.of(
            new PlanNutricionalPlantilla.Etapa(
                EtapaCrianza.PRE_INICIO, 1, 7, new BigDecimal("0.025"), EtapaCrianza.PRE_INICIO, new BigDecimal("40"), null),
            new PlanNutricionalPlantilla.Etapa(
                EtapaCrianza.INICIO, 8, 21, new BigDecimal("0.085"), EtapaCrianza.INICIO, new BigDecimal("40"), null),
            new PlanNutricionalPlantilla.Etapa(
                EtapaCrianza.ENGORDE, 22, 42, new BigDecimal("0.150"), EtapaCrianza.ENGORDE, new BigDecimal("40"), null)));
  }

  @Test
  void creaEditaYListaPlantillas() {
    GestionarPlantillasUseCase useCase =
        new GestionarPlantillasUseCase(repositorio, alimentos, clock);
    PlanNutricionalPlantilla entrada = plantillaValida("ROSS_308");

    PlanNutricionalPlantilla creada = useCase.crear(entrada);
    assertThat(creada.activo()).isTrue();
    verify(repositorio).guardar(any(PlanNutricionalPlantilla.class));

    when(repositorio.buscar(creada.id())).thenReturn(Optional.of(creada));
    PlanNutricionalPlantilla editada = useCase.editar(creada.id(), entrada);
    assertThat(editada.id()).isEqualTo(creada.id());

    when(repositorio.listar()).thenReturn(List.of(editada));
    assertThat(useCase.listar()).hasSize(1);
  }

  @Test
  void rechazaCambioTipoAveOPlantillaInexistente() {
    GestionarPlantillasUseCase useCase =
        new GestionarPlantillasUseCase(repositorio, alimentos, clock);
    UUID id = UUID.randomUUID();
    when(repositorio.buscar(id)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> useCase.consultar(id))
        .isInstanceOf(NoSuchElementException.class);

    PlanNutricionalPlantilla anterior = plantillaValida("ROSS_308");
    when(repositorio.buscar(id)).thenReturn(Optional.of(anterior));
    PlanNutricionalPlantilla otroTipo = plantillaValida("COBB_500");
    assertThatThrownBy(() -> useCase.editar(id, otroTipo))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("tipo de ave");
  }
}
