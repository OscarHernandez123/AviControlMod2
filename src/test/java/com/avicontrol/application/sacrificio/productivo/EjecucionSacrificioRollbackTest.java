package com.avicontrol.application.sacrificio.productivo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.avicontrol.AviControlModulo2Application;
import com.avicontrol.domain.exception.sacrificio.productivo.PrecondicionSacrificioNoCumplidaException;
import com.avicontrol.domain.port.out.modulo1.LoteUpdatePort;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = AviControlModulo2Application.class)
@ActiveProfiles("test")
class EjecucionSacrificioRollbackTest {
  @DynamicPropertySource
  static void baseAislada(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.datasource.url",
        () ->
            "jdbc:h2:mem:ordenes_rollback_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
  }

  @Autowired JdbcTemplate jdbc;
  @Autowired Clock clock;
  @Autowired EjecutarOrdenSacrificioUseCase ejecucion;
  @MockitoBean LoteUpdatePort cambiosLote;

  @Test
  void revierteCambioDeGalponSiNoPuedeFinalizarLote() {
    UUID galpon = UUID.randomUUID();
    UUID lote = UUID.randomUUID();
    UUID orden = UUID.randomUUID();
    Instant ahora = Instant.now(clock);
    jdbc.update(
        "INSERT INTO modulo1_galpon_local (id,lote_id,estado,poblacion_viva,fecha_ingreso,nombre,aforo_maximo,lote_codigo) VALUES (?,?,?,?,?,?,?,?)",
        galpon,
        lote,
        "EN_COSECHA",
        8000,
        LocalDate.now(clock).minusDays(50),
        "G1",
        9000,
        "L1");
    jdbc.update(
        "INSERT INTO ordenes_sacrificio_productivo (id,galpon_id,lote_id,estado,fecha_hora_programada,fecha_creacion,version) VALUES (?,?,?,?,?,?,?)",
        orden,
        galpon,
        lote,
        "PROGRAMADA",
        Timestamp.from(ahora.minusSeconds(10)),
        Timestamp.from(ahora.minusSeconds(100)),
        0);
    assertThrows(PrecondicionSacrificioNoCumplidaException.class, () -> ejecucion.ejecutar(orden));
    assertEquals(
        "EN_COSECHA",
        jdbc.queryForObject(
            "SELECT estado FROM modulo1_galpon_local WHERE id=?", String.class, galpon));
    assertEquals(
        "PROGRAMADA",
        jdbc.queryForObject(
            "SELECT estado FROM ordenes_sacrificio_productivo WHERE id=?", String.class, orden));
  }
}
