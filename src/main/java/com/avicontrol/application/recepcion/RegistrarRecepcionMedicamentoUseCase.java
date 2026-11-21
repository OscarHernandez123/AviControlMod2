package com.avicontrol.application.recepcion;

import com.avicontrol.application.inventario.port.RegistrarEntradaInventarioPort;
import com.avicontrol.domain.exception.recepcion.RecursoNoEncontradoException;
import com.avicontrol.domain.model.catalogo.UnidadMedidaMedicamento;
import com.avicontrol.domain.model.recepcion.RecepcionMedicamento;
import com.avicontrol.domain.port.out.recepcion.RecepcionMedicamentoRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrarRecepcionMedicamentoUseCase {
  private final RecepcionMedicamentoRepositoryPort repository;
  private final RegistrarEntradaInventarioPort inventario;
  private final CatalogoRecepcionPort catalogo;

  public RegistrarRecepcionMedicamentoUseCase(
      RecepcionMedicamentoRepositoryPort repository,
      RegistrarEntradaInventarioPort inventario,
      CatalogoRecepcionPort catalogo) {
    this.repository = repository;
    this.inventario = inventario;
    this.catalogo = catalogo;
  }

  @Transactional
  public RecepcionMedicamento registrar(RegistrarRecepcionMedicamentoCommand command) {
    UnidadMedidaMedicamento base = catalogo.unidadBaseMedicamento(command.medicamentoId());
    if (base == null) throw new RecursoNoEncontradoException("Medicamento");
    RecepcionMedicamento recepcion =
        RecepcionMedicamento.crear(
            command.medicamentoId(),
            catalogo.nombreMedicamento(command.medicamentoId()),
            command.codigoLote(),
            command.presentacion(),
            command.fechaIngreso(),
            command.fechaVencimiento(),
            command.cantidadPresentaciones(),
            command.contenidoPorPresentacion(),
            command.unidadContenido(),
            command.precioNetoPorPresentacion(),
            command.impuestoPorcentaje(),
            command.actor());
    command.unidadContenido().validarCompatibleCon(base);
    repository.guardar(recepcion);
    inventario.medicamento(
        recepcion.id(),
        recepcion.medicamentoId(),
        recepcion.cantidadUnidadBase(),
        recepcion.unidadBase(),
        command.actor());
    return recepcion;
  }
}
