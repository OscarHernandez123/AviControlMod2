package com.avicontrol.infrastructure.adapter.out.integration.modulo1;

import com.avicontrol.domain.exception.modulo1.PoblacionVivaInsuficienteException;
import com.avicontrol.domain.exception.modulo1.ReferenciaLoteInconsistenteException;
import com.avicontrol.domain.port.out.modulo1.ActualizarInventarioVivoPort;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile({"local", "test"})
public class InventarioVivoModulo1Adapter implements ActualizarInventarioVivoPort {
  private final JdbcTemplate jdbc;

  public InventarioVivoModulo1Adapter(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  @Transactional
  public ActualizarInventarioVivoPort.Resultado descontar(
      ActualizarInventarioVivoPort.Descuento c) {
    var existente = repetido(c);
    if (existente.isPresent()) return existente.get();
    var previa =
        jdbc
            .query(
                "SELECT lote_id,poblacion_viva,version FROM modulo1_galpon_local WHERE id=? FOR UPDATE",
                (rs, row) ->
                    new Object[] {rs.getObject(1, UUID.class), rs.getInt(2), rs.getLong(3)},
                c.galponId())
            .stream()
            .findFirst()
            .orElseThrow(ReferenciaLoteInconsistenteException::new);
    existente = repetido(c);
    if (existente.isPresent()) return existente.get();
    if (!c.loteId().equals(previa[0])
        || (c.versionEsperada() >= 0 && c.versionEsperada() != (long) previa[2]))
      throw new ReferenciaLoteInconsistenteException();
    int anterior = (int) previa[1];
    if (anterior < c.cantidadBajas()) throw new PoblacionVivaInsuficienteException();
    int cambiado =
        jdbc.update(
            "UPDATE modulo1_galpon_local SET poblacion_viva=poblacion_viva-?,version=version+1 WHERE id=? AND lote_id=? AND version=? AND poblacion_viva>=?",
            c.cantidadBajas(),
            c.galponId(),
            c.loteId(),
            (long) previa[2],
            c.cantidadBajas());
    if (cambiado != 1) throw new ReferenciaLoteInconsistenteException();
    jdbc.update(
        "INSERT INTO descuentos_inventario_vivo (idempotency_key,lote_id,galpon_id,cantidad,causa,actor,poblacion_anterior,poblacion_resultante) VALUES (?,?,?,?,?,?,?,?)",
        c.idempotencyKey(),
        c.loteId(),
        c.galponId(),
        c.cantidadBajas(),
        c.causa(),
        c.actor(),
        anterior,
        anterior - c.cantidadBajas());
    return new ActualizarInventarioVivoPort.Resultado(
        c.loteId(), anterior, -c.cantidadBajas(), anterior - c.cantidadBajas(), c.causa());
  }

  private java.util.Optional<ActualizarInventarioVivoPort.Resultado> repetido(
      ActualizarInventarioVivoPort.Descuento c) {
    var repetido =
        jdbc
            .query(
                "SELECT lote_id,galpon_id,cantidad,causa,actor,poblacion_anterior,poblacion_resultante FROM descuentos_inventario_vivo WHERE idempotency_key=?",
                (rs, row) ->
                    new Object[] {
                      rs.getObject(1, UUID.class),
                      rs.getObject(2, UUID.class),
                      rs.getInt(3),
                      rs.getString(4),
                      rs.getString(5),
                      rs.getInt(6),
                      rs.getInt(7)
                    },
                c.idempotencyKey())
            .stream()
            .findFirst();
    if (repetido.isPresent()) {
      var r = repetido.get();
      if (!c.loteId().equals(r[0])
          || !c.galponId().equals(r[1])
          || c.cantidadBajas() != (int) r[2]
          || !c.causa().equals(r[3])
          || !c.actor().equals(r[4])) throw new ReferenciaLoteInconsistenteException();
      return java.util.Optional.of(
          new ActualizarInventarioVivoPort.Resultado(
              c.loteId(), (int) r[5], -c.cantidadBajas(), (int) r[6], c.causa()));
    }
    return java.util.Optional.empty();
  }
}
