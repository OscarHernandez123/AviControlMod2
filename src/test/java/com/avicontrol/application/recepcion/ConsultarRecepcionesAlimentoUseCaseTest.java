package com.avicontrol.application.recepcion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.avicontrol.application.inventario.port.AjustarEntradaInventarioPort;
import com.avicontrol.domain.exception.recepcion.RecepcionNoEditableException;
import com.avicontrol.domain.exception.recepcion.RecursoNoEncontradoException;
import com.avicontrol.domain.model.catalogo.EtapaCrianza;
import com.avicontrol.domain.model.recepcion.FiltroRecepciones;
import com.avicontrol.domain.model.recepcion.PaginaRecepciones;
import com.avicontrol.domain.model.recepcion.RecepcionAlimento;
import com.avicontrol.domain.port.out.recepcion.RecepcionAlimentoRepositoryPort;
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
class ConsultarRecepcionesAlimentoUseCaseTest {
  @Mock RecepcionAlimentoRepositoryPort repository;
  @Mock AjustarEntradaInventarioPort inventario;

  private final Clock clock =
      Clock.fixed(Instant.parse("2026-11-20T15:00:00Z"), ZoneId.of("America/Bogota"));

  @Test
  void listaHistorialMensualYPreparaEdicionSiNoHayMovimientos() {
    ConsultarRecepcionesAlimentoUseCase useCase =
        new ConsultarRecepcionesAlimentoUseCase(repository, inventario, clock);
    FiltroRecepciones filtro = new FiltroRecepciones(null, null, null, null, null, 0, 10);
    RecepcionAlimento recepcion =
        RecepcionAlimento.crear(
            UUID.randomUUID(),
            "Preiniciador",
            EtapaCrianza.PRE_INICIO,
            "LOT-10",
            LocalDate.of(2026, 11, 10),
            LocalDate.of(2027, 1, 10),
            5,
            new BigDecimal("40"),
            new BigDecimal("100000"),
            BigDecimal.ZERO,
            "admin");

    when(repository.listar(filtro))
        .thenReturn(new PaginaRecepciones<>(List.of(recepcion), 1, 0, 10));
    when(repository.contarMes(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 1))).thenReturn(1L);
    when(repository.pesoMes(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 1)))
        .thenReturn(new BigDecimal("200"));
    when(repository.buscar(recepcion.id())).thenReturn(Optional.of(recepcion));
    when(inventario.tieneMovimientosPosteriores(recepcion.id())).thenReturn(false);

    HistorialRecepcionesAlimento historial = useCase.listar(filtro);
    assertThat(historial.recepcionesMes()).isEqualTo(1L);
    assertThat(historial.pesoMesKg()).isEqualByComparingTo("200");
    assertThat(historial.pagina().contenido()).hasSize(1);
    assertThat(useCase.prepararEdicion(recepcion.id())).isEqualTo(recepcion);
  }

  @Test
  void lanza404O409EnDetalleYPrepararEdicion() {
    ConsultarRecepcionesAlimentoUseCase useCase =
        new ConsultarRecepcionesAlimentoUseCase(repository, inventario, clock);
    UUID id = UUID.randomUUID();
    when(repository.buscar(id)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> useCase.detalle(id)).isInstanceOf(RecursoNoEncontradoException.class);

    RecepcionAlimento recepcion =
        RecepcionAlimento.crear(
            UUID.randomUUID(),
            "Preiniciador",
            EtapaCrianza.PRE_INICIO,
            "LOT-11",
            LocalDate.of(2026, 11, 10),
            LocalDate.of(2027, 1, 10),
            5,
            new BigDecimal("40"),
            new BigDecimal("100000"),
            BigDecimal.ZERO,
            "admin");
    when(repository.buscar(id)).thenReturn(Optional.of(recepcion));
    when(inventario.tieneMovimientosPosteriores(id)).thenReturn(true);
    assertThatThrownBy(() -> useCase.prepararEdicion(id))
        .isInstanceOf(RecepcionNoEditableException.class);
  }
}
