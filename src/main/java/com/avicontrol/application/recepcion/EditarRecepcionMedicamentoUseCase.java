package com.avicontrol.application.recepcion;

import com.avicontrol.application.inventario.port.AjustarEntradaInventarioPort;
import com.avicontrol.domain.exception.recepcion.RecepcionNoEditableException;
import com.avicontrol.domain.exception.recepcion.RecursoNoEncontradoException;
import com.avicontrol.domain.model.catalogo.UnidadMedidaMedicamento;
import com.avicontrol.domain.model.recepcion.RecepcionMedicamento;
import com.avicontrol.domain.port.out.recepcion.RecepcionMedicamentoRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EditarRecepcionMedicamentoUseCase {
  private final RecepcionMedicamentoRepositoryPort repository;
  private final CatalogoRecepcionPort catalogo;
  private final AjustarEntradaInventarioPort inventario;
  private final AuditoriaRecepcionPort auditoria;

  public EditarRecepcionMedicamentoUseCase(
      RecepcionMedicamentoRepositoryPort repository,
      CatalogoRecepcionPort catalogo,
      AjustarEntradaInventarioPort inventario,
      AuditoriaRecepcionPort auditoria) {
    this.repository = repository;
    this.catalogo = catalogo;
    this.inventario = inventario;
    this.auditoria = auditoria;
  }

  @Transactional
  public RecepcionMedicamento editar(EditarRecepcionMedicamentoCommand command) {
    RecepcionMedicamento anterior =
        repository
            .bloquear(command.id())
            .orElseThrow(() -> new RecursoNoEncontradoException("Recepción de medicamento"));
    if (inventario.tieneMovimientosPosteriores(command.id()))
      throw new RecepcionNoEditableException();
    UnidadMedidaMedicamento base = catalogo.unidadBaseMedicamento(command.medicamentoId());
    if (base == null) throw new RecursoNoEncontradoException("Medicamento");
    command.unidadContenido().validarCompatibleCon(base);
    RecepcionMedicamento nueva =
        anterior.editar(
            command.medicamentoId(),
            catalogo.nombreMedicamento(command.medicamentoId()),
            command.fechaVencimiento(),
            command.cantidadPresentaciones(),
            command.contenidoPorPresentacion(),
            command.unidadContenido(),
            command.precioNetoPorPresentacion(),
            command.impuestoPorcentaje());
    repository.actualizar(nueva);
    inventario.ajustarEntrada(
        nueva.id(),
        "MEDICAMENTO",
        nueva.medicamentoId(),
        nueva.cantidadUnidadBase(),
        nueva.unidadBase().name(),
        command.actor());
    auditoria.medicamento(anterior, nueva, command.actor());
    return nueva;
  }
}
