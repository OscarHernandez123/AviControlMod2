package com.avicontrol.application.recepcion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.avicontrol.application.inventario.port.AjustarEntradaInventarioPort;
import com.avicontrol.domain.exception.recepcion.RecepcionNoEditableException;
import com.avicontrol.domain.exception.recepcion.RecursoNoEncontradoException;
import com.avicontrol.domain.model.catalogo.UnidadMedidaMedicamento;
import com.avicontrol.domain.model.recepcion.FiltroRecepciones;
import com.avicontrol.domain.model.recepcion.PaginaRecepciones;
import com.avicontrol.domain.model.recepcion.RecepcionMedicamento;
import com.avicontrol.domain.port.out.recepcion.RecepcionMedicamentoRepositoryPort;
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

@ExtendWith(MockitoExtension.class)
class ConsultarRecepcionesMedicamentoUseCaseTest {
  @Mock RecepcionMedicamentoRepositoryPort repository;
  @Mock AjustarEntradaInventarioPort inventario;

  private final Clock clock =
      Clock.fixed(Instant.parse("2026-11-20T15:00:00Z"), ZoneId.of("America/Bogota"));

  @Test
  void listaHistorialMensualYPreparaEdicion() {
    ConsultarRecepcionesMedicamentoUseCase useCase =
        new ConsultarRecepcionesMedicamentoUseCase(repository, inventario, clock);
    FiltroRecepciones filtro = new FiltroRecepciones(null, null, null, null, null, 0, 10);
    RecepcionMedicamento recepcion =
        RecepcionMedicamento.crear(
            UUID.randomUUID(),
            "Enrofloxacina",
            "MED-10",
            "Frasco",
            LocalDate.of(2026, 11, 10),
            LocalDate.of(2027, 6, 10),
            2,
            new BigDecimal("100"),
            UnidadMedidaMedicamento.MILILITRO,
            new BigDecimal("45000"),
            BigDecimal.ZERO,
            "admin");

    when(repository.listar(filtro))
        .thenReturn(new PaginaRecepciones<>(List.of(recepcion), 1, 0, 10));
    when(repository.contarMes(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 1)))
        .thenReturn(3L);
    when(repository.buscar(recepcion.id())).thenReturn(Optional.of(recepcion));
    when(inventario.tieneMovimientosPosteriores(recepcion.id())).thenReturn(false);

    HistorialRecepcionesMedicamento historial = useCase.listar(filtro);
    assertThat(historial.recepcionesMes()).isEqualTo(3L);
    assertThat(historial.pagina().contenido()).hasSize(1);
    assertThat(useCase.prepararEdicion(recepcion.id())).isEqualTo(recepcion);
  }

  @Test
  void lanza404O409CuandoNoExisteOTieneMovimientos() {
    ConsultarRecepcionesMedicamentoUseCase useCase =
        new ConsultarRecepcionesMedicamentoUseCase(repository, inventario, clock);
    UUID id = UUID.randomUUID();
    when(repository.buscar(id)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> useCase.detalle(id))
        .isInstanceOf(RecursoNoEncontradoException.class);

    RecepcionMedicamento recepcion =
        RecepcionMedicamento.crear(
            UUID.randomUUID(),
            "Enrofloxacina",
            "MED-11",
            "Frasco",
            LocalDate.of(2026, 11, 10),
            LocalDate.of(2027, 6, 10),
            2,
            new BigDecimal("100"),
            UnidadMedidaMedicamento.MILILITRO,
            new BigDecimal("45000"),
            BigDecimal.ZERO,
            "admin");
    when(repository.buscar(id)).thenReturn(Optional.of(recepcion));
    when(inventario.tieneMovimientosPosteriores(id)).thenReturn(true);
    assertThatThrownBy(() -> useCase.prepararEdicion(id))
        .isInstanceOf(RecepcionNoEditableException.class);
  }
}
