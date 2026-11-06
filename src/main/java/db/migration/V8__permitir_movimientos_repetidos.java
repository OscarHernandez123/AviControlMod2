package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V8__permitir_movimientos_repetidos extends BaseJavaMigration {
  private static final List<String> COLUMNAS =
      List.of("recepcion_id", "tipo_producto", "tipo_movimiento");

  @Override
  public void migrate(Context context) throws Exception {
    Connection connection = context.getConnection();
    List<String> restricciones = new ArrayList<>();
    try (PreparedStatement query =
        connection.prepareStatement(
            "SELECT constraint_name FROM information_schema.table_constraints "
                + "WHERE LOWER(table_name)='movimientos_inventario' AND constraint_type='UNIQUE'")) {
      try (ResultSet rs = query.executeQuery()) {
        while (rs.next()) restricciones.add(rs.getString(1));
      }
    }
    for (String nombre : restricciones) {
      List<String> columnas = new ArrayList<>();
      try (PreparedStatement query =
          connection.prepareStatement(
              "SELECT column_name FROM information_schema.key_column_usage "
                  + "WHERE LOWER(table_name)='movimientos_inventario' AND constraint_name=? "
                  + "ORDER BY ordinal_position")) {
        query.setString(1, nombre);
        try (ResultSet rs = query.executeQuery()) {
          while (rs.next()) columnas.add(rs.getString(1).toLowerCase(Locale.ROOT));
        }
      }
      if (columnas.equals(COLUMNAS)) {
        try (Statement statement = connection.createStatement()) {
          statement.execute(
              "ALTER TABLE movimientos_inventario DROP CONSTRAINT \""
                  + nombre.replace("\"", "\"\"")
                  + "\"");
        }
      }
    }
  }
}
