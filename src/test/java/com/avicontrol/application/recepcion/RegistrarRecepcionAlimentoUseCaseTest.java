package com.avicontrol.application.recepcion;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.avicontrol.application.inventario.port.RegistrarEntradaInventarioPort;
import com.avicontrol.domain.exception.recepcion.RecursoNoEncontradoException;
import com.avicontrol.domain.model.catalogo.EtapaCrianza;
import com.avicontrol.domain.port.out.recepcion.RecepcionAlimentoRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RegistrarRecepcionAlimentoUseCaseTest {
  @Test
  void validaCatalogoAntesDeGuardar() {
    var repo = mock(RecepcionAlimentoRepositoryPort.class);
    var inventario = mock(RegistrarEntradaInventarioPort.class);
    var catalogo = mock(CatalogoRecepcionPort.class);
    var useCase = new RegistrarRecepcionAlimentoUseCase(repo, inventario, catalogo);
    var cmd =
        new RegistrarRecepcionAlimentoCommand(
            UUID.randomUUID(),
            EtapaCrianza.INICIO,
            "AL-1",
            LocalDate.now(),
            LocalDate.now().plusDays(1),
            1,
            BigDecimal.ONE,
            BigDecimal.ONE,
            BigDecimal.ZERO,
            "admin");
    assertThrows(RecursoNoEncontradoException.class, () -> useCase.registrar(cmd));
    verifyNoInteractions(repo, inventario);
  }
}
