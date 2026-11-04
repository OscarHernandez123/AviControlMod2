package com.avicontrol.application.inventario;

import com.avicontrol.application.inventario.port.AjustarEntradaRecepcionPort;
import com.avicontrol.application.support.CurrentActorProvider;
import com.avicontrol.domain.exception.inventario.RecepcionNoEncontradaException;
import com.avicontrol.domain.model.inventario.EstadoMovimientoInventario;
import com.avicontrol.domain.model.inventario.MovimientoAlimento;
import com.avicontrol.domain.model.inventario.MovimientoMedicamento;
import com.avicontrol.domain.model.inventario.TipoMovimientoInventario;
import com.avicontrol.domain.port.out.inventario.MovimientoAlimentoRepositoryPort;
import com.avicontrol.domain.port.out.inventario.MovimientoMedicamentoRepositoryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class AjustarEntradaRecepcionUseCase implements AjustarEntradaRecepcionPort {
  private final MovimientoAlimentoRepositoryPort alimentos;
  private final MovimientoMedicamentoRepositoryPort medicamentos;
  private final CurrentActorProvider actor;
  private final Clock clock;

  public AjustarEntradaRecepcionUseCase(
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
  public void ajustarEntradaAlimento(EntradaAlimentoCommand command) {
    MovimientoAlimento original =
        alimentos
            .buscarEntradaConfirmada(command.recepcionId())
            .orElseThrow(RecepcionNoEncontradaException::new);
    BigDecimal delta = command.cantidadKg().subtract(original.cantidadKg());
    if (delta.signum() != 0)
      alimentos.guardar(
          new MovimientoAlimento(
              UUID.randomUUID(),
              command.recepcionId(),
              TipoMovimientoInventario.AJUSTE,
              delta,
              EstadoMovimientoInventario.CONFIRMADO,
              clock.instant(),
              actor.currentActor().id()));
  }

  @Override
  @Transactional
  public void ajustarEntradaMedicamento(EntradaMedicamentoCommand command) {
    MovimientoMedicamento original =
        medicamentos
            .buscarEntradaConfirmada(command.recepcionId())
            .orElseThrow(RecepcionNoEncontradaException::new);
    if (!original.unidadBase().equals(command.unidadBase()))
      throw new IllegalArgumentException("La unidad de la recepción no puede cambiar");
    BigDecimal delta = command.cantidadUnidadBase().subtract(original.cantidadUnidadBase());
    if (delta.signum() != 0)
      medicamentos.guardar(
          new MovimientoMedicamento(
              UUID.randomUUID(),
              command.recepcionId(),
              TipoMovimientoInventario.AJUSTE,
              delta,
              command.unidadBase(),
              EstadoMovimientoInventario.CONFIRMADO,
              clock.instant(),
              actor.currentActor().id()));
  }
}
