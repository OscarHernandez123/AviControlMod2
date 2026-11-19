package com.avicontrol.infrastructure.adapter.out.integration.modulo1;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.avicontrol.domain.port.out.modulo1.EventosModulo1Port;
import java.time.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

@EnabledIfEnvironmentVariable(named = "AVICONTROL_RUN_POSTGRES_TESTS", matches = "true")
class Modulo1OutboxPostgresTest {
  private final Clock clock = Clock.system(ZoneId.of("America/Bogota"));
  private DriverManagerDataSource base;
  private JdbcTemplate jdbc;
  private TransactionTemplate tx;
  private Modulo1OutboxAdapter repo;
  private String schema;

  @BeforeEach
  void iniciar() throws Exception {
    base =
        new DriverManagerDataSource(
            System.getenv("AVICONTROL_TEST_DB_URL"), "avicontrol", "avicontrol");
    schema = "kafka_test_" + UUID.randomUUID().toString().replace("-", "");
    new JdbcTemplate(base).execute("CREATE SCHEMA " + schema);
    var ds =
        new DriverManagerDataSource(
            System.getenv("AVICONTROL_TEST_DB_URL") + "?currentSchema=" + schema,
            "avicontrol",
            "avicontrol");
    try (var c = ds.getConnection()) {
      ScriptUtils.executeSqlScript(
          c, new ClassPathResource("db/migration/V22__crear_outbox_modulo1.sql"));
      ScriptUtils.executeSqlScript(
          c, new ClassPathResource("db/migration/V23__crear_operaciones_modulo1.sql"));
    }
    jdbc = new JdbcTemplate(ds);
    tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
    repo = new Modulo1OutboxAdapter(jdbc);
  }

  @AfterEach
  void limpiar() {
    if (base != null && schema != null)
      new JdbcTemplate(base).execute("DROP SCHEMA " + schema + " CASCADE");
  }

  @Test
  void rollbackEliminaOutboxYDatosDeNegocioDelFormulario() {
    var e = evento(OffsetDateTime.now(clock));
    var operaciones = new OperacionesModulo1JdbcAdapter(jdbc);
    assertThrows(
        IllegalStateException.class,
        () ->
            tx.executeWithoutResult(
                s -> {
                  repo.encolar(e);
                  operaciones.guardar(
                      new com.avicontrol.domain.port.out.modulo1.OperacionesModulo1Port.Operacion(
                          e.alertaId(),
                          "MORTALIDAD",
                          e.galponId(),
                          UUID.randomUUID(),
                          Map.of("responsable", "trabajador")));
                  throw new IllegalStateException("Fallo posterior antes de confirmar");
                }));
    assertEquals(
        0, jdbc.queryForObject("SELECT count(*) FROM modulo1_event_outbox", Integer.class));
    assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM modulo1_operaciones", Integer.class));
  }

  private EventosModulo1Port.Evento evento(OffsetDateTime fecha) {
    var id = UUID.randomUUID();
    var g = UUID.randomUUID();
    return new EventosModulo1Port.Evento(
        id,
        g,
        "avicontrol.mortalidad",
        "MORTALIDAD_REGISTRADA",
        fecha,
        Map.of(
            "alertaId",
            id.toString(),
            "galponId",
            g.toString(),
            "loteId",
            UUID.randomUUID().toString(),
            "fechaHoraEvento",
            fecha.toString(),
            "cantidadMuertos",
            1));
  }

  @Test
  void rollbackNoDejaEventoPublicable() {
    var e = evento(OffsetDateTime.now(clock));
    assertThrows(
        IllegalStateException.class,
        () ->
            tx.executeWithoutResult(
                s -> {
                  repo.encolar(e);
                  throw new IllegalStateException("rollback");
                }));
    assertEquals(
        0, jdbc.queryForObject("SELECT count(*) FROM modulo1_event_outbox", Integer.class));
  }

  @Test
  void idempotenciaYConflictoPersistidos() {
    var e = evento(OffsetDateTime.now(clock));
    tx.executeWithoutResult(s -> repo.encolar(e));
    tx.executeWithoutResult(s -> repo.encolar(e));
    assertEquals(
        1, jdbc.queryForObject("SELECT count(*) FROM modulo1_event_outbox", Integer.class));
    var distinto =
        new EventosModulo1Port.Evento(
            e.alertaId(),
            e.galponId(),
            e.topic(),
            e.eventType(),
            e.occurredAt(),
            Map.of("cantidadMuertos", 2));
    assertThrows(IllegalStateException.class, () -> repo.repetido(distinto));
  }

  @Test
  @SuppressWarnings("unchecked")
  void falloKafkaConservaEventoYReintentoPublicaMismaClaveYSobre() {
    var e = evento(OffsetDateTime.now(clock));
    tx.executeWithoutResult(s -> repo.encolar(e));
    KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
    when(kafka.send(anyString(), anyString(), anyString()))
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker caído")));
    var publisher = new Modulo1OutboxPublisher(jdbc, kafka, tx, clock);
    publisher.publicar();
    assertEquals("PENDIENTE", repo.consultar(e.alertaId()).estado());
    assertEquals(1, repo.consultar(e.alertaId()).intentos());
    jdbc.update("UPDATE modulo1_event_outbox SET siguiente_intento=CURRENT_TIMESTAMP");
    when(kafka.send(anyString(), anyString(), anyString()))
        .thenReturn(CompletableFuture.completedFuture(null));
    publisher.publicar();
    assertEquals("PUBLICADO", repo.consultar(e.alertaId()).estado());
    assertEquals(2, repo.consultar(e.alertaId()).intentos());
    var values = org.mockito.ArgumentCaptor.forClass(String.class);
    verify(kafka, times(2)).send(eq(e.topic()), eq(e.galponId().toString()), values.capture());
    var json = JsonMapper.builder().build();
    for (String value : values.getAllValues()) {
      var envelope = json.readTree(value);
      assertEquals(e.alertaId().toString(), envelope.path("eventId").asString());
      assertEquals(1, envelope.path("eventVersion").asInt());
      assertEquals(e.eventType(), envelope.path("eventType").asString());
      assertEquals(e.alertaId().toString(), envelope.path("payload").path("alertaId").asString());
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void eventoDelDiaAnteriorExpiraSinPublicarse() {
    var e = evento(OffsetDateTime.now(clock).minusDays(1));
    tx.executeWithoutResult(s -> repo.encolar(e));
    KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
    new Modulo1OutboxPublisher(jdbc, kafka, tx, clock).publicar();
    assertEquals("EXPIRADO", repo.consultar(e.alertaId()).estado());
    verifyNoInteractions(kafka);
  }
}
