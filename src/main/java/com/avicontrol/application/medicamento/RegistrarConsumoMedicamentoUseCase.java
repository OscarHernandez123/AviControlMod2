package com.avicontrol.application.medicamento;

import com.avicontrol.application.medicamento.result.ConsumoMedicamentoResult;
import com.avicontrol.domain.exception.medicamento.CantidadMedicamentoInvalidaException;
import com.avicontrol.domain.exception.medicamento.DiagnosticoConsumoNoEncontradoException;
import com.avicontrol.domain.exception.medicamento.IdempotenciaConsumoIncompatibleException;
import com.avicontrol.domain.exception.medicamento.TratamientoNoActivoException;
import com.avicontrol.domain.model.medicamento.ConsumoMedicamento;
import com.avicontrol.domain.model.medicamento.DetalleConsumoMedicamento;
import com.avicontrol.domain.model.medicamento.UnidadConsumoMedicamento;
import com.avicontrol.domain.model.sanitario.diagnostico.DiagnosticoGalpon;
import com.avicontrol.domain.model.sanitario.diagnostico.EstadoDiagnostico;
import com.avicontrol.domain.port.out.medicamento.ConsumoMedicamentoRepositoryPort;
import com.avicontrol.domain.port.out.medicamento.DiagnosticoQueryPort;
import com.avicontrol.domain.port.out.medicamento.InventarioMedicamentoPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

public class RegistrarConsumoMedicamentoUseCase {
  private final ConsumoMedicamentoRepositoryPort consumos;
  private final DiagnosticoQueryPort diagnosticos;
  private final InventarioMedicamentoPort inventario;
  private final Clock clock;

  public RegistrarConsumoMedicamentoUseCase(
      ConsumoMedicamentoRepositoryPort consumos,
      DiagnosticoQueryPort diagnosticos,
      InventarioMedicamentoPort inventario,
      Clock clock) {
    this.consumos = consumos;
    this.diagnosticos = diagnosticos;
    this.inventario = inventario;
    this.clock = clock;
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public ConsumoMedicamentoResult registrar(RegistrarConsumoMedicamentoCommand command) {
    if (command == null
        || command.diagnosticoId() == null
        || command.unidad() == null
        || command.fechaAplicacion() == null
        || command.idempotenciaClave() == null
        || command.actor() == null
        || command.actor().isBlank())
      throw new CantidadMedicamentoInvalidaException("Datos obligatorios del consumo incompletos");
    BigDecimal normalizada = command.unidad().normalizar(command.cantidad());
    var previo =
        consumos.buscarPorIdempotencia(command.diagnosticoId(), command.idempotenciaClave());
    if (previo.isPresent()) {
      ConsumoMedicamento c = previo.get();
      if (c.cantidadIngresada().compareTo(command.cantidad()) != 0
          || c.unidadIngresada() != command.unidad()
          || !c.fechaAplicacion().equals(command.fechaAplicacion())
          || !c.actor().equals(command.actor()))
        throw new IdempotenciaConsumoIncompatibleException();
      return ConsumoMedicamentoResult.de(c);
    }
    DiagnosticoGalpon d =
        diagnosticos
            .buscar(command.diagnosticoId())
            .orElseThrow(DiagnosticoConsumoNoEncontradoException::new);
    LocalDate hoy = LocalDate.now(clock);
    if (d.estado() != EstadoDiagnostico.ACTIVO
        || d.requiereSacrificioSanitario()
        || d.medicamentoId() == null
        || d.fechaReintegro() == null
        || !hoy.isBefore(d.fechaReintegro())) throw new TratamientoNoActivoException();
    if (command.fechaAplicacion().isBefore(d.fechaDiagnostico())
        || command.fechaAplicacion().isAfter(hoy)
        || !command.fechaAplicacion().isBefore(d.fechaReintegro()))
      throw new CantidadMedicamentoInvalidaException("Fecha de aplicación fuera del tratamiento");
    UnidadConsumoMedicamento base = command.unidad().unidadBase();
    List<DetalleConsumoMedicamento> detalles =
        inventario.reservarFefo(d.medicamentoId(), normalizada, base, hoy);
    ConsumoMedicamento consumo =
        new ConsumoMedicamento(
            UUID.randomUUID(),
            d.id(),
            d.galponId(),
            d.loteId(),
            d.medicamentoId(),
            command.cantidad(),
            command.unidad(),
            normalizada,
            base,
            command.fechaAplicacion(),
            Instant.now(clock),
            command.actor(),
            command.idempotenciaClave(),
            detalles);
    consumos.guardar(consumo);
    return ConsumoMedicamentoResult.de(consumo);
  }
}
