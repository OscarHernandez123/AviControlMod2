package com.avicontrol.application.recepcion;

import com.avicontrol.application.inventario.port.AjustarEntradaInventarioPort;
import com.avicontrol.domain.exception.recepcion.RecepcionNoEditableException;
import com.avicontrol.domain.exception.recepcion.RecursoNoEncontradoException;
import com.avicontrol.domain.model.recepcion.RecepcionAlimento;
import com.avicontrol.domain.port.out.recepcion.RecepcionAlimentoRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EditarRecepcionAlimentoUseCase {
  private final RecepcionAlimentoRepositoryPort repository;
  private final CatalogoRecepcionPort catalogo;
  private final AjustarEntradaInventarioPort inventario;
  private final AuditoriaRecepcionPort auditoria;

  public EditarRecepcionAlimentoUseCase(
      RecepcionAlimentoRepositoryPort repository,
      CatalogoRecepcionPort catalogo,
      AjustarEntradaInventarioPort inventario,
      AuditoriaRecepcionPort auditoria) {
    this.repository = repository;
    this.catalogo = catalogo;
    this.inventario = inventario;
    this.auditoria = auditoria;
  }

  @Transactional
  public RecepcionAlimento editar(EditarRecepcionAlimentoCommand command) {
    RecepcionAlimento anterior =
        repository
            .bloquear(command.id())
            .orElseThrow(() -> new RecursoNoEncontradoException("Recepción de alimento"));
    if (inventario.tieneMovimientosPosteriores(command.id()))
      throw new RecepcionNoEditableException();
    if (anterior.tipoAlimento() == null)
      throw new RecepcionNoEditableException(
          "La recepción histórica no tiene tipo de alimento registrado");
    if (!catalogo.alimentoExiste(command.alimentoId()))
      throw new RecursoNoEncontradoException("Alimento");
    RecepcionAlimento nueva =
        anterior.editar(
            command.alimentoId(),
            catalogo.nombreAlimento(command.alimentoId()),
            command.fechaVencimiento(),
            command.cantidadBultos(),
            command.pesoPorBultoKg(),
            command.precioNetoPorBulto(),
            command.impuestoPorcentaje());
    repository.actualizar(nueva);
    inventario.ajustarEntrada(
        nueva.id(), "ALIMENTO", nueva.alimentoId(), nueva.cantidadTotalKg(), "KG", command.actor());
    auditoria.alimento(anterior, nueva, command.actor());
    return nueva;
  }
}
