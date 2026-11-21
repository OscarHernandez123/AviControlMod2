package com.avicontrol.application.recepcion;

import com.avicontrol.application.inventario.port.AjustarEntradaInventarioPort;
import com.avicontrol.domain.exception.recepcion.RecepcionNoEditableException;
import com.avicontrol.domain.exception.recepcion.RecursoNoEncontradoException;
import com.avicontrol.domain.model.recepcion.FiltroRecepciones;
import com.avicontrol.domain.model.recepcion.RecepcionAlimento;
import com.avicontrol.domain.port.out.recepcion.RecepcionAlimentoRepositoryPort;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultarRecepcionesAlimentoUseCase {
  private final RecepcionAlimentoRepositoryPort repository;
  private final AjustarEntradaInventarioPort inventario;
  private final Clock clock;

  public ConsultarRecepcionesAlimentoUseCase(
      RecepcionAlimentoRepositoryPort repository,
      AjustarEntradaInventarioPort inventario,
      Clock clock) {
    this.repository = repository;
    this.inventario = inventario;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public HistorialRecepcionesAlimento listar(FiltroRecepciones filtro) {
    LocalDate hoy = LocalDate.now(clock);
    LocalDate inicioMes = hoy.withDayOfMonth(1);
    return new HistorialRecepcionesAlimento(
        repository.listar(filtro),
        repository.contarMes(inicioMes, inicioMes.plusMonths(1)),
        repository.pesoMes(inicioMes, inicioMes.plusMonths(1)));
  }

  @Transactional(readOnly = true)
  public RecepcionAlimento detalle(UUID id) {
    return repository
        .buscar(id)
        .orElseThrow(() -> new RecursoNoEncontradoException("Recepción de alimento"));
  }

  @Transactional(readOnly = true)
  public RecepcionAlimento prepararEdicion(UUID id) {
    RecepcionAlimento recepcion = detalle(id);
    if (inventario.tieneMovimientosPosteriores(id)) throw new RecepcionNoEditableException();
    return recepcion;
  }
}
