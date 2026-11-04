package com.avicontrol.application.inventario;

import com.avicontrol.application.inventario.port.RegistrarEntradaInventarioPort;
import com.avicontrol.application.support.CurrentActorProvider;
import com.avicontrol.domain.exception.inventario.MovimientoInvalidoException;
import com.avicontrol.domain.model.inventario.EstadoMovimientoInventario;
import com.avicontrol.domain.model.inventario.MovimientoAlimento;
import com.avicontrol.domain.model.inventario.MovimientoMedicamento;
import com.avicontrol.domain.model.inventario.TipoMovimientoInventario;
import com.avicontrol.domain.port.out.inventario.MovimientoAlimentoRepositoryPort;
import com.avicontrol.domain.port.out.inventario.MovimientoMedicamentoRepositoryPort;
import java.time.Clock;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class RegistrarEntradaInventarioUseCase implements RegistrarEntradaInventarioPort {
  private final MovimientoAlimentoRepositoryPort alimentos;
  private final MovimientoMedicamentoRepositoryPort medicamentos;
  private final CurrentActorProvider actor;
  private final Clock clock;

  public RegistrarEntradaInventarioUseCase(
      MovimientoAlimentoRepositoryPort alimentos,
      MovimientoMedicamentoRepositoryPort medicamentos,
      CurrentActorProvider actor,
      Clock clock) {
    this.alimentos = alimentos;
    this.medicamentos = medicamentos;
    this.actor = actor;
    this.clock = clock;
  }

  @Override
  @Transactional
  public void registrarEntradaAlimento(EntradaAlimentoCommand command) {
    if (alimentos.buscarEntradaConfirmada(command.recepcionId()).isPresent())
      throw new MovimientoInvalidoException("La recepción ya tiene una entrada confirmada");
    alimentos.guardar(
        new MovimientoAlimento(
            UUID.randomUUID(),
            command.recepcionId(),
            TipoMovimientoInventario.ENTRADA,
            command.cantidadKg(),
            EstadoMovimientoInventario.CONFIRMADO,
            clock.instant(),
            actor.currentActor().id()));
  }

  @Override
  @Transactional
  public void registrarEntradaMedicamento(EntradaMedicamentoCommand command) {
    if (medicamentos.buscarEntradaConfirmada(command.recepcionId()).isPresent())
      throw new MovimientoInvalidoException("La recepción ya tiene una entrada confirmada");
    medicamentos.guardar(
        new MovimientoMedicamento(
            UUID.randomUUID(),
            command.recepcionId(),
            TipoMovimientoInventario.ENTRADA,
            command.cantidadUnidadBase(),
            command.unidadBase(),
            EstadoMovimientoInventario.CONFIRMADO,
            clock.instant(),
            actor.currentActor().id()));
  }
}
