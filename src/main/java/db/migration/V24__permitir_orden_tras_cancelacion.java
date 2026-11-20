package db.migration;

import java.sql.Connection;
import java.util.ArrayList;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Conserva canceladas y reserva una sola orden no cancelada por galpón/lote. */
public class V24__permitir_orden_tras_cancelacion extends BaseJavaMigration {
  @Override
  public void migrate(Context context) throws Exception {
    Connection connection = context.getConnection();
    var constraints = new ArrayList<String>();
    try (var query = connection.createStatement();
        var rows =
            query.executeQuery(
                """
            SELECT tc.constraint_name
            FROM information_schema.table_constraints tc
            JOIN information_schema.key_column_usage k
              ON tc.constraint_catalog=k.constraint_catalog
              AND tc.constraint_schema=k.constraint_schema
              AND tc.constraint_name=k.constraint_name
              AND tc.table_name=k.table_name
            WHERE tc.table_schema='public'
              AND tc.table_name='ordenes_sacrificio_productivo'
              AND tc.constraint_type='UNIQUE'
            GROUP BY tc.constraint_name
            HAVING COUNT(*)=2
              AND SUM(CASE WHEN k.column_name IN ('galpon_id','lote_id') THEN 1 ELSE 0 END)=2
            """)) {
      while (rows.next()) constraints.add(rows.getString(1));
    }
    if (constraints.size() != 1) {
      throw new IllegalStateException("Se esperaba una restricción única por galpón/lote");
    }
    try (var statement = connection.createStatement()) {
      statement.execute(
          "ALTER TABLE ordenes_sacrificio_productivo ADD COLUMN lote_reservado_id UUID");
      statement.execute(
          """
          UPDATE ordenes_sacrificio_productivo SET lote_reservado_id=lote_id
          WHERE estado<>'CANCELADA'
          """);
      String name = constraints.getFirst().replace("\"", "\"\"");
      statement.execute(
          "ALTER TABLE ordenes_sacrificio_productivo DROP CONSTRAINT \"" + name + "\"");
      statement.execute(
          """
          ALTER TABLE ordenes_sacrificio_productivo ADD CONSTRAINT ck_orden_reserva_lote
          CHECK ((estado='CANCELADA' AND lote_reservado_id IS NULL)
            OR (estado<>'CANCELADA' AND lote_reservado_id IS NOT NULL AND lote_reservado_id=lote_id))
          """);
      statement.execute(
          """
          CREATE UNIQUE INDEX uq_orden_no_cancelada_galpon_lote
          ON ordenes_sacrificio_productivo (galpon_id,lote_reservado_id)
          """);
    }
  }
}
