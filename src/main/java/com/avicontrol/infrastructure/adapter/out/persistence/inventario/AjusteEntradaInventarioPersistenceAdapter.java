package com.avicontrol.infrastructure.adapter.out.persistence.inventario;

import com.avicontrol.application.inventario.port.AjustarEntradaInventarioPort;
import com.avicontrol.domain.exception.recepcion.RecursoNoEncontradoException;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class AjusteEntradaInventarioPersistenceAdapter implements AjustarEntradaInventarioPort {
  private final MovimientoInventarioJpaRepository repository;

  public AjusteEntradaInventarioPersistenceAdapter(MovimientoInventarioJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  public boolean tieneMovimientosPosteriores(UUID recepcionId) {
    return repository.contarPosteriores(recepcionId) > 0;
  }

  @Override
  public void ajustarEntrada(
      UUID recepcionId,
      String tipoProducto,
      UUID productoId,
      BigDecimal cantidad,
      String unidad,
      String actor) {
    MovimientoInventarioEntity entrada =
        repository
            .entradaInicial(recepcionId, tipoProducto)
            .orElseThrow(() -> new RecursoNoEncontradoException("Entrada inicial de inventario"));
    entrada.ajustarEntrada(productoId, cantidad, unidad, actor);
    repository.saveAndFlush(entrada);
  }
}
