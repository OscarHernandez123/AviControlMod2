package com.avicontrol.application.mortalidad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.avicontrol.AviControlModulo2Application;
import com.avicontrol.application.alerta.GenerarAlertaSanitariaUseCase;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = AviControlModulo2Application.class)
@ActiveProfiles("test")
class MortalidadAlertaRollbackTest {
  @Autowired RegistrarMortalidadAnormalUseCase reportes;
  @Autowired JdbcTemplate jdbc;
  @Autowired Clock clock;
  @MockitoBean GenerarAlertaSanitariaUseCase alertas;

  @Test
  void fallaDeAlertaRevierteElReporteAnormal() {
    UUID galpon = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO modulo1_galpon_local (id,lote_id,estado,poblacion_viva,fecha_ingreso,nombre,aforo_maximo,lote_codigo) VALUES (?,?,?,?,?,?,?,?)",
        galpon,
        UUID.randomUUID(),
        "PRODUCTIVA",
        5000,
        LocalDate.now(clock).minusDays(40),
        "Galpón prueba",
        10000,
        "Lote prueba");
    when(alertas.generar(any())).thenThrow(new IllegalStateException("Alerta no disponible"));
    assertThrows(
        IllegalStateException.class,
        () ->
            reportes.registrar(
                new RegistrarMortalidadAnormalCommand(
                    galpon,
                    Instant.now(clock).minusSeconds(60),
                    18,
                    "RESPIRATORIA",
                    "Hallazgo",
                    "operario",
                    UUID.randomUUID())));
    assertEquals(
        0,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM registros_mortalidad_anormal WHERE galpon_id=?",
            Integer.class,
            galpon));
  }
}
