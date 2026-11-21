package com.avicontrol.infrastructure.adapter.in.rest.medicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avicontrol.AviControlModulo2Application;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = AviControlModulo2Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MedicacionDiariaControllerTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired Clock clock;

  @Test
  void consultaTratamientoYResumenSinModificarDiagnosticoNiInventario() throws Exception {
    LocalDate hoy = LocalDate.now(clock);
    UUID galpon = UUID.randomUUID();
    UUID lote = UUID.randomUUID();
    UUID enfermedad = UUID.randomUUID();
    UUID medicamento = UUID.randomUUID();
    UUID medicacion = UUID.randomUUID();
    UUID diagnostico = UUID.randomUUID();
    Instant ahora = Instant.now(clock);
    jdbc.update(
        "INSERT INTO modulo1_galpon_local (id, lote_id, estado, poblacion_viva, fecha_ingreso, nombre, aforo_maximo, lote_codigo) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
        galpon,
        lote,
        "AISLAMIENTO",
        8000,
        hoy.minusDays(12),
        "Galpón 5",
        10000,
        "LDP-005");
    jdbc.update(
        "INSERT INTO enfermedades (id, nombre, nivel_riesgo, tipo, descripcion_clinica, requiere_sacrificio_sanitario, version, fecha_creacion, fecha_actualizacion) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
        enfermedad,
        "Coccidiosis",
        "ALTO",
        "PARASITARIA",
        "Signos",
        false,
        0,
        ahora,
        ahora);
    jdbc.update(
        "INSERT INTO medicaciones (id, enfermedad_id, medicamento_id, dosis, duracion_dias, descripcion, version, fecha_creacion, fecha_actualizacion) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
        medicacion,
        enfermedad,
        medicamento,
        "200 g",
        5,
        "Agua de bebida",
        0,
        ahora,
        ahora);
    jdbc.update(
        "INSERT INTO diagnosticos_galpon (id, galpon_id, active_galpon_id, lote_id, enfermedad_id, enfermedad_nombre, requiere_sacrificio_sanitario, medicacion_id, medicamento_id, dosis, duracion_dias, descripcion_medicacion, observaciones, fecha_diagnostico, fecha_reintegro, estado, fecha_resolucion, version) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        diagnostico,
        galpon,
        galpon,
        lote,
        enfermedad,
        "Coccidiosis",
        false,
        medicacion,
        medicamento,
        "200 g",
        5,
        "Agua de bebida",
        "Tratamiento",
        hoy.minusDays(1),
        hoy.plusDays(4),
        "ACTIVO",
        null,
        0);
    jdbc.update(
        "INSERT INTO recepciones_medicamento (id, medicamento_id, codigo_lote, presentacion, fecha_ingreso, fecha_vencimiento, cantidad_presentaciones, contenido_por_presentacion, unidad_contenido, precio_neto_por_presentacion, impuesto_porcentaje, cantidad_unidad_base, unidad_base, precio_neto_unidad_base, precio_neto_total, valor_impuesto, precio_total, estado, actor, nombre_medicamento) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        UUID.randomUUID(),
        medicamento,
        "RX-1",
        "Frasco",
        hoy.minusDays(2),
        hoy.plusMonths(1),
        1,
        100,
        "GRAMO",
        1000,
        0,
        100,
        "GRAMO",
        10,
        1000,
        0,
        1000,
        "CONFIRMADA",
        "admin",
        "Amprolio 20% Solución");

    mvc.perform(
            get("/api/galpones/{id}/medicacion-diaria", galpon)
                .with(SecurityMockMvcRequestPostProcessors.user("worker").roles("TRABAJADOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombreGalpon").value("Galpón 5"))
        .andExpect(jsonPath("$.loteCodigo").value("LDP-005"))
        .andExpect(jsonPath("$.poblacionViva").value(8000))
        .andExpect(jsonPath("$.tratamientos[0].progreso").value("Día 2 de 5"))
        .andExpect(jsonPath("$.tratamientos[0].enfermedad").value("Coccidiosis"))
        .andExpect(jsonPath("$.tratamientos[0].medicamento").value("Amprolio 20% Solución"))
        .andExpect(jsonPath("$.tratamientos[0].dosisHoy").value("200 g"));
    mvc.perform(
            get("/api/medicacion-diaria/resumen")
                .param("galponIds", galpon.toString())
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.galponesConTratamiento").value(1))
        .andExpect(jsonPath("$.cantidadTratamientos").value(1))
        .andExpect(
            jsonPath("$.galpones[0].tratamientos[0].viaAdministracion").value("Agua de bebida"));
    assertEquals(
        0,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM consumos_medicamento WHERE diagnostico_id = ?",
            Integer.class,
            diagnostico));
    assertEquals(
        0,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM movimientos_inventario WHERE producto_id = ?",
            Integer.class,
            medicamento));
  }

  @Test
  void respuestasVaciasErroresYSeguridad() throws Exception {
    UUID galpon = UUID.randomUUID();
    UUID lote = UUID.randomUUID();
    LocalDate hoy = LocalDate.now(clock);
    jdbc.update(
        "INSERT INTO modulo1_galpon_local (id, lote_id, estado, poblacion_viva, fecha_ingreso, nombre, aforo_maximo, lote_codigo) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
        galpon,
        lote,
        "PRODUCTIVA",
        100,
        hoy.minusDays(5),
        "Galpón sano",
        200,
        "L-SANO");
    mvc.perform(
            get("/api/galpones/{id}/medicacion-diaria", galpon)
                .with(SecurityMockMvcRequestPostProcessors.user("worker").roles("TRABAJADOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("SIN_MEDICACION_ACTIVA"))
        .andExpect(jsonPath("$.tratamientos").isEmpty());
    mvc.perform(
            get("/api/medicacion-diaria/resumen")
                .with(SecurityMockMvcRequestPostProcessors.user("worker").roles("TRABAJADOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.galponesConTratamiento").value(0))
        .andExpect(jsonPath("$.galpones").isEmpty());
    mvc.perform(
            get("/api/galpones/{id}/medicacion-diaria", UUID.randomUUID())
                .with(SecurityMockMvcRequestPostProcessors.user("worker").roles("TRABAJADOR")))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/galpones/{id}/medicacion-diaria", galpon))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            get("/api/galpones/{id}/medicacion-diaria", galpon)
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().isForbidden());
    UUID sinLote = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO modulo1_galpon_local (id, lote_id, estado, poblacion_viva, fecha_ingreso, nombre, aforo_maximo, lote_codigo) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
        sinLote,
        null,
        "PRODUCTIVA",
        0,
        null,
        "Galpón sin lote",
        200,
        null);
    mvc.perform(
            get("/api/galpones/{id}/medicacion-diaria", sinLote)
                .with(SecurityMockMvcRequestPostProcessors.user("worker").roles("TRABAJADOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.loteId").value((Object) null))
        .andExpect(jsonPath("$.estado").value("SIN_MEDICACION_ACTIVA"));
  }
}
