package com.avicontrol.infrastructure.adapter.out.integration.modulo1;

import com.avicontrol.domain.model.galpon.EstadoGalpon;
import com.avicontrol.domain.port.out.modulo1.GalponQueryPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"local", "test"})
public class GalponQueryAdapter implements GalponQueryPort {
  private final JdbcTemplate jdbc;

  public GalponQueryAdapter(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<String> estadoVigente(UUID galponId) {
    return jdbc
        .query(
            "SELECT estado FROM modulo1_galpon_local WHERE id = ?",
            (rs, row) -> rs.getString(1),
            galponId)
        .stream()
        .findFirst();
  }

  @Override
  public Optional<GalponDatos> consultar(UUID galponId) {
    return jdbc
        .query(
            "SELECT id,nombre,aforo_maximo,estado FROM modulo1_galpon_local WHERE id = ?",
            (rs, row) -> map(rs),
            galponId)
        .stream()
        .findFirst();
  }

  @Override
  public List<GalponDatos> todos() {
    return jdbc.query(
        "SELECT id,nombre,aforo_maximo,estado FROM modulo1_galpon_local", (rs, row) -> map(rs));
  }

  @Override
  public PaginaGalpones listar(int pagina, int tamano, String campoOrden, boolean ascendente) {
    String columna =
        switch (campoOrden) {
          case "nombre" -> "nombre";
          case "estado" -> "estado";
          case "id" -> "id";
          default -> throw new IllegalArgumentException("Orden inválido");
        };
    String direccion = ascendente ? " ASC" : " DESC";
    String sql =
        "SELECT id,nombre,aforo_maximo,estado FROM modulo1_galpon_local ORDER BY "
            + columna
            + direccion
            + ", id ASC LIMIT ? OFFSET ?";
    List<GalponDatos> elementos =
        jdbc.query(sql, (rs, row) -> map(rs), tamano, Math.multiplyExact(pagina, tamano));
    Long total = jdbc.queryForObject("SELECT COUNT(*) FROM modulo1_galpon_local", Long.class);
    return new PaginaGalpones(elementos, total == null ? 0 : total);
  }

  private GalponDatos map(java.sql.ResultSet rs) throws java.sql.SQLException {
    return new GalponDatos(
        rs.getObject("id", UUID.class),
        rs.getString("nombre"),
        rs.getInt("aforo_maximo"),
        EstadoGalpon.desde(rs.getString("estado"))
            .map(EstadoGalpon::name)
            .orElse(rs.getString("estado")));
  }
}
