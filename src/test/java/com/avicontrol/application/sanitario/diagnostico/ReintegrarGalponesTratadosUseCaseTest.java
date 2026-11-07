package com.avicontrol.application.sanitario.diagnostico;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.avicontrol.AviControlModulo2Application;
import com.avicontrol.domain.model.sanitario.diagnostico.DiagnosticoGalpon;
import com.avicontrol.domain.model.sanitario.enfermedad.Enfermedad;
import com.avicontrol.domain.model.sanitario.enfermedad.NivelRiesgo;
import com.avicontrol.domain.model.sanitario.enfermedad.TipoEnfermedad;
import com.avicontrol.domain.model.sanitario.medicacion.Medicacion;
import com.avicontrol.domain.port.out.sanitario.diagnostico.DiagnosticoGalponRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = AviControlModulo2Application.class)
@ActiveProfiles("test")
class ReintegrarGalponesTratadosUseCaseTest {
  @Autowired ReintegrarGalponesTratadosUseCase useCase;
  @Autowired DiagnosticoGalponRepositoryPort diagnosticos;
  @Autowired JdbcTemplate jdbc;
  @Autowired Clock clock;

  @BeforeEach
  void preparar() {
    jdbc.update("DELETE FROM diagnosticos_galpon");
  }

  @Test
  void reintegraVencidoUnaVez() {
    UUID galpon = UUID.randomUUID();
    UUID lote = UUID.randomUUID();
    insertarGalpon(galpon, lote, "AISLAMIENTO");
    DiagnosticoGalpon d = diagnosticoVencido(galpon, lote);
    assertEquals(1, useCase.ejecutar());
    assertEquals("PRODUCTIVA", estadoGalpon(galpon));
    assertEquals("REINTEGRADO", estadoDiagnostico(d.id()));
    assertEquals(0, useCase.ejecutar());
  }

  @Test
  void noSobrescribeEstadoDistinto() {
    UUID galpon = UUID.randomUUID();
    UUID lote = UUID.randomUUID();
    insertarGalpon(galpon, lote, "MANTENIMIENTO");
    DiagnosticoGalpon d = diagnosticoVencido(galpon, lote);
    assertEquals(0, useCase.ejecutar());
    assertEquals("MANTENIMIENTO", estadoGalpon(galpon));
    assertEquals("REINTEGRO_OMITIDO", estadoDiagnostico(d.id()));
  }

  @Test
  void recuperaReintegroVencidoCuandoRegresaModulo1() {
    UUID galpon = UUID.randomUUID();
    UUID lote = UUID.randomUUID();
    DiagnosticoGalpon d = diagnosticoVencido(galpon, lote);
    assertEquals(0, useCase.ejecutar());
    assertEquals("ACTIVO", estadoDiagnostico(d.id()));
    insertarGalpon(galpon, lote, "AISLAMIENTO");
    assertEquals(1, useCase.ejecutar());
    assertEquals("REINTEGRADO", estadoDiagnostico(d.id()));
  }

  private DiagnosticoGalpon diagnosticoVencido(UUID galpon, UUID lote) {
    Instant now = Instant.now(clock);
    Enfermedad e =
        new Enfermedad(
            UUID.randomUUID(),
            "Newcastle",
            NivelRiesgo.ALTO,
            TipoEnfermedad.VIRAL,
            "Signos",
            false,
            0,
            now,
            now);
    Medicacion m =
        new Medicacion(
            UUID.randomUUID(), e.id(), UUID.randomUUID(), "0.5 ml", 5, "Aplicar", 0, now, now);
    jdbc.update(
        "INSERT INTO enfermedades (id, nombre, nivel_riesgo, tipo, descripcion_clinica, requiere_sacrificio_sanitario, version, fecha_creacion, fecha_actualizacion) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
        e.id(),
        e.nombre(),
        e.nivelRiesgo().name(),
        e.tipo().name(),
        e.descripcionClinica(),
        false,
        0,
        now,
        now);
    jdbc.update(
        "INSERT INTO medicaciones (id, enfermedad_id, medicamento_id, dosis, duracion_dias, descripcion, version, fecha_creacion, fecha_actualizacion) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
        m.id(),
        e.id(),
        m.medicamentoId(),
        m.dosis(),
        m.duracionDias(),
        m.descripcion(),
        0,
        now,
        now);
    DiagnosticoGalpon d =
        DiagnosticoGalpon.crear(
            galpon, lote, e, m, "Tratamiento", LocalDate.now(clock).minusDays(10));
    diagnosticos.crear(d);
    return d;
  }

  private void insertarGalpon(UUID galpon, UUID lote, String estado) {
    jdbc.update(
        "INSERT INTO modulo1_galpon_local (id, lote_id, estado, poblacion_viva, fecha_ingreso) VALUES (?, ?, ?, ?, ?)",
        galpon,
        lote,
        estado,
        8000,
        LocalDate.now(clock).minusDays(10));
  }

  private String estadoGalpon(UUID galpon) {
    return jdbc.queryForObject(
        "SELECT estado FROM modulo1_galpon_local WHERE id = ?", String.class, galpon);
  }

  private String estadoDiagnostico(UUID id) {
    return jdbc.queryForObject(
        "SELECT estado FROM diagnosticos_galpon WHERE id = ?", String.class, id);
  }
}
