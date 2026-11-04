package com.avicontrol.application.inventario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.avicontrol.application.support.CurrentActor;
import com.avicontrol.domain.exception.inventario.MovimientoInvalidoException;
import com.avicontrol.domain.model.inventario.MovimientoAlimento;
import com.avicontrol.domain.model.inventario.MovimientoMedicamento;
import com.avicontrol.domain.port.out.inventario.MovimientoAlimentoRepositoryPort;
import com.avicontrol.domain.port.out.inventario.MovimientoMedicamentoRepositoryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RegistrarEntradaInventarioUseCaseTest {

  @Test
  void createsOnlyOneConfirmedEntryPerReception() {
    Map<UUID, MovimientoAlimento> food = new HashMap<>();
    var useCase =
        new RegistrarEntradaInventarioUseCase(
            foodRepository(food),
            medicineRepository(),
            () -> new CurrentActor("admin", java.util.Set.of("ROLE_ADMINISTRADOR")),
            Clock.fixed(Instant.parse("2026-11-03T12:00:00Z"), ZoneOffset.UTC));
    UUID recepcionId = UUID.randomUUID();

    useCase.registrarEntradaAlimento(
        new EntradaAlimentoCommand(recepcionId, new BigDecimal("40.0000")));

    assertThat(food).hasSize(1);
    assertThatThrownBy(
            () ->
                useCase.registrarEntradaAlimento(
                    new EntradaAlimentoCommand(recepcionId, BigDecimal.ONE)))
        .isInstanceOf(MovimientoInvalidoException.class);
  }

  private MovimientoAlimentoRepositoryPort foodRepository(Map<UUID, MovimientoAlimento> values) {
    return new MovimientoAlimentoRepositoryPort() {
      @Override
      public MovimientoAlimento guardar(MovimientoAlimento movimiento) {
        values.put(movimiento.recepcionId(), movimiento);
        return movimiento;
      }

      @Override
      public Optional<MovimientoAlimento> buscarEntradaConfirmada(UUID recepcionId) {
        return Optional.ofNullable(values.get(recepcionId));
      }
    };
  }

  private MovimientoMedicamentoRepositoryPort medicineRepository() {
    return new MovimientoMedicamentoRepositoryPort() {
      @Override
      public MovimientoMedicamento guardar(MovimientoMedicamento movimiento) {
        return movimiento;
      }

      @Override
      public Optional<MovimientoMedicamento> buscarEntradaConfirmada(UUID recepcionId) {
        return Optional.empty();
      }
    };
  }
}
