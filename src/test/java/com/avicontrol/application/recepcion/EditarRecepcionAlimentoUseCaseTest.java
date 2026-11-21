package com.avicontrol.application.recepcion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.avicontrol.application.inventario.port.AjustarEntradaInventarioPort;
import com.avicontrol.domain.exception.recepcion.RecepcionNoEditableException;
import com.avicontrol.domain.exception.recepcion.RecursoNoEncontradoException;
import com.avicontrol.domain.model.catalogo.EtapaCrianza;
import com.avicontrol.domain.model.recepcion.RecepcionAlimento;
import com.avicontrol.domain.port.out.recepcion.RecepcionAlimentoRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EditarRecepcionAlimentoUseCaseTest {
  @Mock RecepcionAlimentoRepositoryPort repository;
  @Mock CatalogoRecepcionPort catalogo;
  @Mock AjustarEntradaInventarioPort inventario;
  @Mock AuditoriaRecepcionPort auditoria;
  @InjectMocks EditarRecepcionAlimentoUseCase useCase;

  @Test
  void editaRecepcionAjustaInventarioYRegistraAuditoria() {
    UUID id = UUID.randomUUID();
    UUID alimentoId = UUID.randomUUID();
    RecepcionAlimento anterior =
        RecepcionAlimento.crear(
            alimentoId,
            "Iniciador",
            EtapaCrianza.INICIO,
            "LOT-01",
            LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 12, 31),
            10,
            new BigDecimal("40"),
            new BigDecimal("100000"),
            new BigDecimal("5"),
            "admin");
    EditarRecepcionAlimentoCommand cmd =
        new EditarRecepcionAlimentoCommand(
            id,
            alimentoId,
            LocalDate.of(2027, 1, 15),
            12,
            new BigDecimal("40"),
            new BigDecimal("105000"),
            new BigDecimal("5"),
            "admin");

    when(repository.bloquear(id)).thenReturn(Optional.of(anterior));
    when(inventario.tieneMovimientosPosteriores(id)).thenReturn(false);
    when(catalogo.alimentoExiste(alimentoId)).thenReturn(true);
    when(catalogo.nombreAlimento(alimentoId)).thenReturn("Iniciador Plus");

    RecepcionAlimento resultado = useCase.editar(cmd);

    assertThat(resultado.cantidadBultos()).isEqualTo(12);
    assertThat(resultado.cantidadTotalKg()).isEqualByComparingTo("480");
    assertThat(resultado.nombreAlimento()).isEqualTo("Iniciador Plus");
    verify(repository).actualizar(resultado);
    verify(inventario)
        .ajustarEntrada(
            eq(anterior.id()),
            eq("ALIMENTO"),
            eq(alimentoId),
            eq(resultado.cantidadTotalKg()),
            eq("KG"),
            eq("admin"));
    verify(auditoria).alimento(anterior, resultado, "admin");
  }

  @Test
  void rechazaCuandoNoExisteOTieneMovimientosPosteriores() {
    UUID id = UUID.randomUUID();
    EditarRecepcionAlimentoCommand cmd =
        new EditarRecepcionAlimentoCommand(
            id,
            UUID.randomUUID(),
            LocalDate.of(2027, 1, 15),
            5,
            new BigDecimal("40"),
            new BigDecimal("90000"),
            BigDecimal.ZERO,
            "admin");

    when(repository.bloquear(id)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> useCase.editar(cmd)).isInstanceOf(RecursoNoEncontradoException.class);

    RecepcionAlimento existente =
        RecepcionAlimento.crear(
            cmd.alimentoId(),
            "Iniciador",
            EtapaCrianza.INICIO,
            "LOT-02",
            LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 12, 31),
            5,
            new BigDecimal("40"),
            new BigDecimal("90000"),
            BigDecimal.ZERO,
            "admin");
    when(repository.bloquear(id)).thenReturn(Optional.of(existente));
    when(inventario.tieneMovimientosPosteriores(id)).thenReturn(true);

    assertThatThrownBy(() -> useCase.editar(cmd)).isInstanceOf(RecepcionNoEditableException.class);
    verifyNoInteractions(auditoria);
  }

  @Test
  void rechazaCuandoAlimentoCatalogoNoExiste() {
    UUID id = UUID.randomUUID();
    UUID alimentoId = UUID.randomUUID();
    RecepcionAlimento existente =
        RecepcionAlimento.crear(
            alimentoId,
            "Iniciador",
            EtapaCrianza.INICIO,
            "LOT-03",
            LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 12, 31),
            5,
            new BigDecimal("40"),
            new BigDecimal("90000"),
            BigDecimal.ZERO,
            "admin");
    when(repository.bloquear(id)).thenReturn(Optional.of(existente));
    when(inventario.tieneMovimientosPosteriores(id)).thenReturn(false);
    when(catalogo.alimentoExiste(alimentoId)).thenReturn(false);

    EditarRecepcionAlimentoCommand cmd =
        new EditarRecepcionAlimentoCommand(
            id,
            alimentoId,
            LocalDate.of(2027, 1, 15),
            5,
            new BigDecimal("40"),
            new BigDecimal("90000"),
            BigDecimal.ZERO,
            "admin");

    assertThatThrownBy(() -> useCase.editar(cmd)).isInstanceOf(RecursoNoEncontradoException.class);
    verify(repository, org.mockito.Mockito.never()).actualizar(any());
  }
}
