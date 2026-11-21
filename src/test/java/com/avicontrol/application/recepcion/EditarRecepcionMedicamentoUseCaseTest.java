package com.avicontrol.application.recepcion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.avicontrol.application.inventario.port.AjustarEntradaInventarioPort;
import com.avicontrol.domain.exception.recepcion.RecepcionNoEditableException;
import com.avicontrol.domain.exception.recepcion.RecursoNoEncontradoException;
import com.avicontrol.domain.exception.recepcion.UnidadIncompatibleException;
import com.avicontrol.domain.model.catalogo.UnidadMedidaMedicamento;
import com.avicontrol.domain.model.recepcion.RecepcionMedicamento;
import com.avicontrol.domain.port.out.recepcion.RecepcionMedicamentoRepositoryPort;
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
class EditarRecepcionMedicamentoUseCaseTest {
  @Mock RecepcionMedicamentoRepositoryPort repository;
  @Mock CatalogoRecepcionPort catalogo;
  @Mock AjustarEntradaInventarioPort inventario;
  @Mock AuditoriaRecepcionPort auditoria;
  @InjectMocks EditarRecepcionMedicamentoUseCase useCase;

  @Test
  void editaRecepcionMedicamentoConvierteUnidadBaseYAjustaInventario() {
    UUID id = UUID.randomUUID();
    UUID medId = UUID.randomUUID();
    RecepcionMedicamento anterior =
        RecepcionMedicamento.crear(
            medId,
            "Enrofloxacina",
            "MED-01",
            "Frasco",
            LocalDate.of(2026, 11, 1),
            LocalDate.of(2027, 6, 30),
            2,
            new BigDecimal("100"),
            UnidadMedidaMedicamento.MILILITRO,
            new BigDecimal("50000"),
            BigDecimal.ZERO,
            "admin");
    EditarRecepcionMedicamentoCommand cmd =
        new EditarRecepcionMedicamentoCommand(
            id,
            medId,
            LocalDate.of(2027, 8, 30),
            3,
            new BigDecimal("1000"),
            UnidadMedidaMedicamento.MILIGRAMO,
            new BigDecimal("200000"),
            new BigDecimal("19"),
            "admin");

    when(repository.bloquear(id)).thenReturn(Optional.of(anterior));
    when(inventario.tieneMovimientosPosteriores(id)).thenReturn(false);
    when(catalogo.unidadBaseMedicamento(medId)).thenReturn(UnidadMedidaMedicamento.MILILITRO);
    when(catalogo.nombreMedicamento(medId)).thenReturn("Enrofloxacina 10%");

    RecepcionMedicamento resultado = useCase.editar(cmd);

    assertThat(resultado.cantidadUnidadBase()).isEqualByComparingTo("3.000");
    assertThat(resultado.unidadBase()).isEqualTo(UnidadMedidaMedicamento.GRAMO);
    verify(repository).actualizar(resultado);
    verify(inventario)
        .ajustarEntrada(
            eq(anterior.id()),
            eq("MEDICAMENTO"),
            eq(medId),
            eq(resultado.cantidadUnidadBase()),
            eq("GRAMO"),
            eq("admin"));
    verify(auditoria).medicamento(anterior, resultado, "admin");
  }

  @Test
  void rechazaUnidadIncompatibleOMovimientosPosterioresONoEncontrado() {
    UUID id = UUID.randomUUID();
    UUID medId = UUID.randomUUID();
    EditarRecepcionMedicamentoCommand cmd =
        new EditarRecepcionMedicamentoCommand(
            id,
            medId,
            LocalDate.of(2027, 8, 30),
            2,
            new BigDecimal("100"),
            UnidadMedidaMedicamento.GRAMO,
            new BigDecimal("50000"),
            BigDecimal.ZERO,
            "admin");

    when(repository.bloquear(id)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> useCase.editar(cmd))
        .isInstanceOf(RecursoNoEncontradoException.class);

    RecepcionMedicamento anterior =
        RecepcionMedicamento.crear(
            medId,
            "Enrofloxacina",
            "MED-01",
            "Frasco",
            LocalDate.of(2026, 11, 1),
            LocalDate.of(2027, 6, 30),
            2,
            new BigDecimal("100"),
            UnidadMedidaMedicamento.MILILITRO,
            new BigDecimal("50000"),
            BigDecimal.ZERO,
            "admin");
    when(repository.bloquear(id)).thenReturn(Optional.of(anterior));
    when(inventario.tieneMovimientosPosteriores(id)).thenReturn(false);
    when(catalogo.unidadBaseMedicamento(medId)).thenReturn(UnidadMedidaMedicamento.MILILITRO);

    assertThatThrownBy(() -> useCase.editar(cmd))
        .isInstanceOf(UnidadIncompatibleException.class);

    when(inventario.tieneMovimientosPosteriores(id)).thenReturn(true);
    assertThatThrownBy(() -> useCase.editar(cmd))
        .isInstanceOf(RecepcionNoEditableException.class);
    verifyNoInteractions(auditoria);
  }
}
