package com.avicontrol.application.recepcion;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.avicontrol.application.catalogo.port.AlimentoCatalogoQueryPort;
import com.avicontrol.application.catalogo.port.MedicamentoCatalogoQueryPort;
import com.avicontrol.application.support.CurrentActor;
import com.avicontrol.application.support.CurrentActorProvider;
import com.avicontrol.domain.exception.recepcion.RecepcionUtilizadaException;
import com.avicontrol.domain.model.catalogo.*;
import com.avicontrol.domain.model.recepcion.*;
import com.avicontrol.domain.port.out.recepcion.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class GestionarRecepcionesUseCaseTest {
  @Test
  void registraEntradaDeAlimentoYPublicaEvento() {
    RecepcionAlimentoRepositoryPort repo = mock();
    AlimentoCatalogoQueryPort catalogo = mock();
    InventarioRecepcionPort inventario = mock();
    RecepcionEventPublisherPort eventos = mock();
    UUID productoId = UUID.randomUUID();
    when(catalogo.buscarAlimentoActivo(productoId))
        .thenReturn(
            Optional.of(
                new Alimento(
                    productoId,
                    "Inicio",
                    "Marca",
                    TipoAlimento.INICIO,
                    BigDecimal.TEN,
                    null,
                    EstadoProductoCatalogo.ACTIVO)));
    when(repo.guardar(any())).thenAnswer(i -> i.getArgument(0));
    GestionarRecepcionesUseCase useCase =
        useCase(repo, mock(), catalogo, mock(), inventario, eventos);

    useCase.registrarAlimento(
        new RecepcionAlimentoCommand(
            productoId,
            TipoAlimento.INICIO,
            "L-1",
            LocalDate.now(),
            LocalDate.now().plusDays(1),
            BigDecimal.TWO,
            BigDecimal.TEN,
            BigDecimal.TEN,
            BigDecimal.ZERO));

    verify(inventario).registrarAlimento(any(), eq(new BigDecimal("20.0000")));
    verify(eventos).publish(any(GestionarRecepcionesUseCase.RecepcionAlimentoConfirmadaV1.class));
  }

  @Test
  void bloqueaEdicionDeRecepcionUtilizada() {
    RecepcionAlimentoRepositoryPort repo = mock();
    InventarioRecepcionPort inventario = mock();
    UUID id = UUID.randomUUID();
    when(repo.buscarPorId(id)).thenReturn(Optional.of(alimento(id)));
    when(inventario.alimentoUtilizado(id)).thenReturn(true);
    GestionarRecepcionesUseCase useCase = useCase(repo, mock(), mock(), mock(), inventario, mock());

    assertThatThrownBy(
            () ->
                useCase.editarAlimento(
                    id,
                    new RecepcionAlimentoCommand(
                        UUID.randomUUID(),
                        TipoAlimento.INICIO,
                        "PROTEGIDO",
                        LocalDate.now(),
                        LocalDate.now().plusDays(2),
                        BigDecimal.ONE,
                        BigDecimal.ONE,
                        BigDecimal.ONE,
                        BigDecimal.ZERO)))
        .isInstanceOf(RecepcionUtilizadaException.class);
    verify(repo, never()).guardar(any());
  }

  private GestionarRecepcionesUseCase useCase(
      RecepcionAlimentoRepositoryPort alimentos,
      RecepcionMedicamentoRepositoryPort medicamentos,
      AlimentoCatalogoQueryPort catalogoAlimentos,
      MedicamentoCatalogoQueryPort catalogoMedicamentos,
      InventarioRecepcionPort inventario,
      RecepcionEventPublisherPort eventos) {
    CurrentActorProvider actor = () -> new CurrentActor("admin", Set.of("ROLE_ADMINISTRADOR"));
    return new GestionarRecepcionesUseCase(
        alimentos,
        medicamentos,
        catalogoAlimentos,
        catalogoMedicamentos,
        inventario,
        mock(AuditoriaRecepcionRepositoryPort.class),
        actor,
        Clock.systemUTC(),
        eventos);
  }

  private RecepcionAlimento alimento(UUID id) {
    return new RecepcionAlimento(
        id,
        UUID.randomUUID(),
        "Inicio",
        TipoAlimento.INICIO,
        "L-1",
        LocalDate.now(),
        LocalDate.now().plusDays(1),
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.ZERO,
        EstadoRecepcion.CONFIRMADA);
  }
}
