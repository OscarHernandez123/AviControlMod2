package com.avicontrol.application.recepcion;

import com.avicontrol.application.inventario.port.AjustarEntradaInventarioPort;
import com.avicontrol.domain.exception.recepcion.RecepcionNoEditableException;
import com.avicontrol.domain.exception.recepcion.RecursoNoEncontradoException;
import com.avicontrol.domain.model.recepcion.FiltroRecepciones;
import com.avicontrol.domain.model.recepcion.RecepcionMedicamento;
import com.avicontrol.domain.port.out.recepcion.RecepcionMedicamentoRepositoryPort;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultarRecepcionesMedicamentoUseCase {
  private final RecepcionMedicamentoRepositoryPort repository;
  private final AjustarEntradaInventarioPort inventario;
  private final Clock clock;

  public ConsultarRecepcionesMedicamentoUseCase(
      RecepcionMedicamentoRepositoryPort repository,
      AjustarEntradaInventarioPort inventario,
      Clock clock) {
    this.repository = repository;
    this.inventario = inventario;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public HistorialRecepcionesMedicamento listar(FiltroRecepciones filtro) {
    LocalDate hoy = LocalDate.now(clock);
    LocalDate inicioMes = hoy.withDayOfMonth(1);
    return new HistorialRecepcionesMedicamento(
        repository.listar(filtro), repository.contarMes(inicioMes, inicioMes.plusMonths(1)));
  }

  @Transactional(readOnly = true)
  public RecepcionMedicamento detalle(UUID id) {
    return repository
        .buscar(id)
        .orElseThrow(() -> new RecursoNoEncontradoException("Recepción de medicamento"));
  }

  @Transactional(readOnly = true)
  public RecepcionMedicamento prepararEdicion(UUID id) {
    RecepcionMedicamento recepcion = detalle(id);
    if (inventario.tieneMovimientosPosteriores(id)) throw new RecepcionNoEditableException();
    return recepcion;
  }
}
