package com.avicontrol.infrastructure.adapter.out.persistence.inventario;

import com.avicontrol.application.inventario.port.AjustarEntradaInventarioPort;
import com.avicontrol.domain.exception.recepcion.RecursoNoEncontradoException;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AjusteEntradaInventarioPersistenceAdapter implements AjustarEntradaInventarioPort {
  private final MovimientoInventarioJpaRepository repository;
  private final JdbcTemplate jdbc;

  public AjusteEntradaInventarioPersistenceAdapter(
      MovimientoInventarioJpaRepository repository, JdbcTemplate jdbc) {
    this.repository = repository;
    this.jdbc = jdbc;
  }

  @Override
  public boolean tieneMovimientosPosteriores(UUID recepcionId) {
    Integer consumos =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM detalles_consumo_medicamento WHERE recepcion_id = ?",
            Integer.class,
            recepcionId);
    Integer suministros =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM detalles_suministro_alimento WHERE recepcion_id = ?",
            Integer.class,
            recepcionId);
    return repository.contarPosteriores(recepcionId) > 0
        || (consumos != null && consumos > 0)
        || (suministros != null && suministros > 0);
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
