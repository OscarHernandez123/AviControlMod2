package com.avicontrol.infrastructure.adapter.out.persistence.sacrificio.productivo;

import com.avicontrol.domain.exception.sacrificio.productivo.PrecondicionSacrificioNoCumplidaException;
import com.avicontrol.domain.exception.sacrificio.productivo.ResultadoSacrificioDuplicadoException;
import com.avicontrol.domain.model.sacrificio.productivo.EstadoResultadoSacrificio;
import com.avicontrol.domain.model.sacrificio.productivo.ResultadoSacrificioProductivo;
import com.avicontrol.domain.port.out.sacrificio.productivo.ResultadoSacrificioRepositoryPort;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ResultadoSacrificioPersistenceAdapter implements ResultadoSacrificioRepositoryPort {
  private final JdbcTemplate jdbc;

  public ResultadoSacrificioPersistenceAdapter(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void crear(ResultadoSacrificioProductivo resultado) {
    try {
      jdbc.update(
          "INSERT INTO resultados_sacrificio_productivo (id,orden_id,galpon_id,lote_id,cantidad_final_pollos,peso_total_kg,estado,fecha_registro,version) VALUES (?,?,?,?,?,?,?,?,?)",
          resultado.id(),
          resultado.ordenId(),
          resultado.galponId(),
          resultado.loteId(),
          resultado.cantidadFinalPollos(),
          resultado.pesoTotalKg(),
          resultado.estado().name(),
          Timestamp.from(resultado.fechaRegistro()),
          resultado.version());
    } catch (DataIntegrityViolationException exception) {
      throw new ResultadoSacrificioDuplicadoException();
    }
  }

  @Override
  public Optional<ResultadoSacrificioProductivo> buscarPorOrden(UUID ordenId) {
    return jdbc
        .query(
            "SELECT * FROM resultados_sacrificio_productivo WHERE orden_id=?",
            (rs, row) -> map(rs),
            ordenId)
        .stream()
        .findFirst();
  }

  @Override
  public Optional<ResultadoSacrificioProductivo> buscar(UUID resultadoId) {
    return jdbc
        .query(
            "SELECT * FROM resultados_sacrificio_productivo WHERE id=?",
            (rs, row) -> map(rs),
            resultadoId)
        .stream()
        .findFirst();
  }

  @Override
  public Optional<ResultadoSacrificioProductivo> bloquear(UUID resultadoId) {
    return jdbc
        .query(
            "SELECT * FROM resultados_sacrificio_productivo WHERE id=? FOR UPDATE",
            (rs, row) -> map(rs),
            resultadoId)
        .stream()
        .findFirst();
  }

  @Override
  public boolean existePorLote(UUID loteId) {
    Integer total =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM resultados_sacrificio_productivo WHERE lote_id=?",
            Integer.class,
            loteId);
    return total != null && total > 0;
  }

  @Override
  public boolean actualizarSiDisponible(ResultadoSacrificioProductivo resultado) {
    return jdbc.update(
            "UPDATE resultados_sacrificio_productivo SET cantidad_final_pollos=?,peso_total_kg=?,fecha_correccion=?,version=? WHERE id=? AND estado='DISPONIBLE' AND version=?",
            resultado.cantidadFinalPollos(),
            resultado.pesoTotalKg(),
            Timestamp.from(resultado.fechaCorreccion()),
            resultado.version(),
            resultado.id(),
            resultado.version() - 1)
        == 1;
  }

  @Override
  public boolean marcarUtilizado(UUID resultadoId) {
    return jdbc.update(
            "UPDATE resultados_sacrificio_productivo SET estado='UTILIZADO',version=version+1 WHERE id=? AND estado='DISPONIBLE'",
            resultadoId)
        == 1;
  }

  @Override
  public boolean registrarConfirmacion(UUID eventoId, UUID resultadoId) {
    UUID existente =
        jdbc.query(
            "SELECT resultado_id FROM confirmaciones_uso_resultado_sacrificio WHERE evento_id=?",
            rs -> rs.next() ? rs.getObject(1, UUID.class) : null,
            eventoId);
    if (existente != null) {
      if (!resultadoId.equals(existente)) throw new PrecondicionSacrificioNoCumplidaException();
      return false;
    }
    try {
      jdbc.update(
          "INSERT INTO confirmaciones_uso_resultado_sacrificio (evento_id,resultado_id) VALUES (?,?)",
          eventoId,
          resultadoId);
      return true;
    } catch (DataIntegrityViolationException exception) {
      throw new PrecondicionSacrificioNoCumplidaException();
    }
  }

  private ResultadoSacrificioProductivo map(ResultSet rs) throws SQLException {
    Timestamp correccion = rs.getTimestamp("fecha_correccion");
    return new ResultadoSacrificioProductivo(
        rs.getObject("id", UUID.class),
        rs.getObject("orden_id", UUID.class),
        rs.getObject("galpon_id", UUID.class),
        rs.getObject("lote_id", UUID.class),
        rs.getInt("cantidad_final_pollos"),
        rs.getBigDecimal("peso_total_kg"),
        EstadoResultadoSacrificio.valueOf(rs.getString("estado")),
        rs.getTimestamp("fecha_registro").toInstant(),
        correccion == null ? null : correccion.toInstant(),
        rs.getLong("version"));
  }
}
