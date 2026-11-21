package com.avicontrol.application.recepcion;

import com.avicontrol.application.inventario.port.RegistrarEntradaInventarioPort;
import com.avicontrol.domain.exception.recepcion.RecursoNoEncontradoException;
import com.avicontrol.domain.model.recepcion.RecepcionAlimento;
import com.avicontrol.domain.port.out.recepcion.RecepcionAlimentoRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrarRecepcionAlimentoUseCase {
  private final RecepcionAlimentoRepositoryPort repository;
  private final RegistrarEntradaInventarioPort inventario;
  private final CatalogoRecepcionPort catalogo;

  public RegistrarRecepcionAlimentoUseCase(
      RecepcionAlimentoRepositoryPort repository,
      RegistrarEntradaInventarioPort inventario,
      CatalogoRecepcionPort catalogo) {
    this.repository = repository;
    this.inventario = inventario;
    this.catalogo = catalogo;
  }

  @Transactional
  public RecepcionAlimento registrar(RegistrarRecepcionAlimentoCommand command) {
    if (!catalogo.alimentoExiste(command.alimentoId())) {
      throw new RecursoNoEncontradoException("Alimento");
    }
    RecepcionAlimento recepcion =
        RecepcionAlimento.crear(
            command.alimentoId(),
            catalogo.nombreAlimento(command.alimentoId()),
            command.tipoAlimento(),
            command.codigoLote(),
            command.fechaIngreso(),
            command.fechaVencimiento(),
            command.cantidadBultos(),
            command.pesoPorBultoKg(),
            command.precioNetoPorBulto(),
            command.impuestoPorcentaje(),
            command.actor());
    repository.guardar(recepcion);
    inventario.alimento(
        recepcion.id(), recepcion.alimentoId(), recepcion.cantidadTotalKg(), command.actor());
    return recepcion;
  }
}
