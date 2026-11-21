package com.avicontrol.application.recepcion;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.avicontrol.application.inventario.port.RegistrarEntradaInventarioPort;
import com.avicontrol.domain.exception.recepcion.UnidadIncompatibleException;
import com.avicontrol.domain.model.catalogo.UnidadMedidaMedicamento;
import com.avicontrol.domain.port.out.recepcion.RecepcionMedicamentoRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RegistrarRecepcionMedicamentoUseCaseTest {
  @Test
  void unidadIncompatibleNoGuardaNada() {
    var repo = mock(RecepcionMedicamentoRepositoryPort.class);
    var inventario = mock(RegistrarEntradaInventarioPort.class);
    var catalogo = mock(CatalogoRecepcionPort.class);
    UUID id = UUID.randomUUID();
    when(catalogo.unidadBaseMedicamento(id)).thenReturn(UnidadMedidaMedicamento.UNIDAD);
    when(catalogo.nombreMedicamento(id)).thenReturn("Medicamento de prueba");
    var cmd =
        new RegistrarRecepcionMedicamentoCommand(
            id,
            "MED-1",
            "Caja",
            LocalDate.now(),
            LocalDate.now().plusDays(1),
            1,
            BigDecimal.ONE,
            UnidadMedidaMedicamento.GRAMO,
            BigDecimal.ONE,
            BigDecimal.ZERO,
            "admin");
    assertThrows(
        UnidadIncompatibleException.class,
        () -> new RegistrarRecepcionMedicamentoUseCase(repo, inventario, catalogo).registrar(cmd));
    verifyNoInteractions(repo, inventario);
  }
}
