package com.avicontrol.infrastructure.adapter.in.rest.sacrificio.productivo;

import static org.junit.jupiter.api.Assertions.*;

import com.avicontrol.domain.model.sacrificio.productivo.OrdenSacrificioProductivo;
import com.avicontrol.infrastructure.adapter.out.persistence.sacrificio.productivo.OrdenSacrificioProductivoPersistenceAdapter;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@EnabledIfEnvironmentVariable(named = "AVICONTROL_RUN_POSTGRES_TESTS", matches = "true")
class OrdenTrasCancelacionPostgresTest {
  @Test
  void migraHistorialExistenteYPermiteSoloUnaNuevaOrdenConcurrente() throws Exception {
    var url = System.getenv("AVICONTROL_TEST_DB_URL");
    var ds = new DriverManagerDataSource(url, "avicontrol", "avicontrol");
    Flyway.configure()
        .dataSource(ds)
        .locations("classpath:db/migration", "classpath:db/vendor/postgresql")
        .target("17")
        .load()
        .migrate();
    var jdbc = new JdbcTemplate(ds);
    UUID galpon = UUID.randomUUID(), lote = UUID.randomUUID(), anterior = UUID.randomUUID();
    jdbc.update(
        """
        INSERT INTO ordenes_sacrificio_productivo
        (id,galpon_id,lote_id,estado,fecha_hora_programada,fecha_creacion,fecha_cancelacion)
        VALUES (?,?,?,'CANCELADA',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
        """,
        anterior,
        galpon,
        lote);
    Flyway.configure()
        .dataSource(ds)
        .locations("classpath:db/migration", "classpath:db/vendor/postgresql")
        .load()
        .migrate();
    var repo = new OrdenSacrificioProductivoPersistenceAdapter(jdbc);
    assertFalse(repo.existePorGalponLote(galpon, lote));
    assertEquals("CANCELADA", repo.buscar(anterior).orElseThrow().estado().name());
    var inicio = new CountDownLatch(1);
    var ahora = Instant.now();
    try (var pool = Executors.newFixedThreadPool(2)) {
      java.util.concurrent.Callable<Boolean> crear =
          () -> {
            inicio.await();
            try {
              repo.crear(
                  OrdenSacrificioProductivo.programar(
                      galpon, lote, ahora.plusSeconds(3600), ahora));
              return true;
            } catch (DataIntegrityViolationException expected) {
              return false;
            }
          };
      var primera = pool.submit(crear);
      var segunda = pool.submit(crear);
      inicio.countDown();
      assertEquals(
          1,
          (primera.get(10, TimeUnit.SECONDS) ? 1 : 0)
              + (segunda.get(10, TimeUnit.SECONDS) ? 1 : 0));
    }
    assertTrue(repo.existePorGalponLote(galpon, lote));
    assertEquals(2, repo.resumen().totalOrdenes());
    assertEquals(1, repo.resumen().ordenesPendientes());

    // Simula el historial de la V24 Java ya aplicada: mismo esquema, tipo JDBC y sin checksum.
    var datosAntes = jdbc.queryForList("SELECT * FROM ordenes_sacrificio_productivo ORDER BY id");
    jdbc.update(
        """
        UPDATE flyway_schema_history SET type='JDBC',
          script='db.migration.V24__permitir_orden_tras_cancelacion',checksum=NULL
        WHERE version='24'
        """);
    var flyway =
        Flyway.configure()
            .dataSource(ds)
            .locations("classpath:db/migration", "classpath:db/vendor/postgresql")
            .load();
    assertFalse(flyway.validateWithResult().validationSuccessful);
    try (var connection = ds.getConnection();
        var statement = connection.createStatement()) {
      var sql =
          new org.springframework.core.io.ClassPathResource(
                  "db/maintenance/normalizar_historial_v24_java_a_sql.sql")
              .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
      statement.execute(sql);
    }
    flyway.repair();
    flyway.validate();
    assertEquals(0, flyway.migrate().migrationsExecuted);
    assertEquals(
        datosAntes, jdbc.queryForList("SELECT * FROM ordenes_sacrificio_productivo ORDER BY id"));
    assertEquals(
        "SQL",
        jdbc.queryForObject(
            "SELECT type FROM flyway_schema_history WHERE version='24'", String.class));
  }
}
