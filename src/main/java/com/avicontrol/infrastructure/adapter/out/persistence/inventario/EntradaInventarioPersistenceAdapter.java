package com.avicontrol.infrastructure.adapter.out.persistence.inventario;

import com.avicontrol.application.inventario.port.RegistrarEntradaInventarioPort;
import com.avicontrol.domain.model.catalogo.UnidadMedidaMedicamento;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class EntradaInventarioPersistenceAdapter implements RegistrarEntradaInventarioPort {
  private final MovimientoInventarioJpaRepository repository;

  public EntradaInventarioPersistenceAdapter(MovimientoInventarioJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  public void alimento(UUID recepcionId, UUID alimentoId, BigDecimal kg, String actor) {
    repository.saveAndFlush(
        new MovimientoInventarioEntity(recepcionId, alimentoId, "ALIMENTO", kg, "KG", actor));
  }

  @Override
  public void medicamento(
      UUID recepcionId,
      UUID medicamentoId,
      BigDecimal cantidad,
      UnidadMedidaMedicamento unidadBase,
      String actor) {
    repository.saveAndFlush(
        new MovimientoInventarioEntity(
            recepcionId, medicamentoId, "MEDICAMENTO", cantidad, unidadBase.name(), actor));
  }
}
