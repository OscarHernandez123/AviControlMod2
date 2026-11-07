package com.avicontrol.infrastructure.adapter.in.rest.medicacion;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avicontrol.AviControlModulo2Application;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = AviControlModulo2Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MedicacionDiariaFlujoTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired Clock clock;
  private final ObjectMapper json = new ObjectMapper();

  @Test
  void dosDiagnosticosConservanIdentidadYCalendarioDePrescripcion() throws Exception {
    LocalDate hoy = LocalDate.now(clock);
    UUID galpon = UUID.randomUUID();
    UUID lote = UUID.randomUUID();
    UUID medicamento = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    jdbc.update(
        "INSERT INTO modulo1_galpon_local (id, lote_id, estado, poblacion_viva, fecha_ingreso, nombre, aforo_maximo, lote_codigo) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
        galpon,
        lote,
        "AISLAMIENTO",
        5000,
        hoy.minusDays(10),
        "Galpón 5",
        6000,
        "LDP-005");
    inventarioDisponible(medicamento, hoy);
    UUID enfermedadUno = enfermedad("Coccidiosis");
    UUID enfermedadDos = enfermedad("Bacteriosis");
    UUID medicacionUno =
        medicacion(enfermedadUno, medicamento, "Amprolio 20%", "Amprolio", "[1,3,5]");
    UUID medicacionDos =
        medicacion(enfermedadDos, medicamento, "Antibiótico B", "Compuesto B", "null");
    UUID diagnosticoUno = diagnostico(galpon, enfermedadUno, medicacionUno);
    diagnostico(galpon, enfermedadDos, medicacionDos);
    mvc.perform(
            post("/api/galpones/{id}/diagnosticos", galpon)
                .contentType(MediaType.APPLICATION_JSON)
                .content(diagnosticoBody(enfermedadUno, medicacionUno))
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().isConflict());

    mvc.perform(
            get("/api/galpones/{id}/medicacion-diaria", galpon)
                .with(SecurityMockMvcRequestPostProcessors.user("worker").roles("TRABAJADOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cantidadTratamientos").value(2))
        .andExpect(jsonPath("$.tratamientos[*].principioActivo").value(hasItem("Amprolio")))
        .andExpect(jsonPath("$.tratamientos[*].principioActivo").value(hasItem("Compuesto B")))
        .andExpect(jsonPath("$.tratamientos[*].dosisHoy").value(hasItem("200 g")));
    mvc.perform(
            get("/api/galpones/{id}/medicacion-diaria", galpon)
                .param("fecha", hoy.plusDays(1).toString())
                .with(SecurityMockMvcRequestPostProcessors.user("worker").roles("TRABAJADOR")))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.tratamientos[?(@.principioActivo == 'Amprolio')].aplicaHoy")
                .value(hasItem(false)))
        .andExpect(
            jsonPath("$.tratamientos[?(@.principioActivo == 'Amprolio')].dosisHoy")
                .value(hasItem("0")))
        .andExpect(
            jsonPath("$.tratamientos[?(@.principioActivo == 'Compuesto B')].aplicaHoy")
                .value(hasItem(true)));

    mvc.perform(
            put("/api/medicaciones/{id}", medicacionUno)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    medicacionBody(
                        enfermedadUno, medicamento, "Nombre nuevo", "Principio nuevo", "[2,4]"))
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().isOk());
    mvc.perform(
            get("/api/diagnosticos/{id}", diagnosticoUno)
                .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.medicamentoNombre").value("Amprolio 20%"))
        .andExpect(jsonPath("$.principioActivo").value("Amprolio"))
        .andExpect(jsonPath("$.diasAplicacion[1]").value(3));
    assertEquals(
        2,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM diagnosticos_galpon WHERE galpon_id = ? AND estado = 'ACTIVO'",
            Integer.class,
            galpon));
  }

  private UUID enfermedad(String nombre) {
    UUID id = UUID.randomUUID();
    Instant ahora = Instant.now(clock);
    jdbc.update(
        "INSERT INTO enfermedades (id, nombre, nivel_riesgo, tipo, descripcion_clinica, requiere_sacrificio_sanitario, version, fecha_creacion, fecha_actualizacion) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
        id,
        nombre,
        "ALTO",
        "BACTERIANA",
        "Signos",
        false,
        0,
        ahora,
        ahora);
    return id;
  }

  private UUID medicacion(
      UUID enfermedad, UUID medicamento, String nombre, String principio, String dias)
      throws Exception {
    String response =
        mvc.perform(
                post("/api/medicaciones")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(medicacionBody(enfermedad, medicamento, nombre, principio, dias))
                    .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return UUID.fromString(json.readTree(response).get("id").asText());
  }

  private UUID diagnostico(UUID galpon, UUID enfermedad, UUID medicacion) throws Exception {
    String response =
        mvc.perform(
                post("/api/galpones/{id}/diagnosticos", galpon)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(diagnosticoBody(enfermedad, medicacion))
                    .with(SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO")))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return UUID.fromString(json.readTree(response).get("id").asText());
  }

  private String diagnosticoBody(UUID enfermedad, UUID medicacion) {
    return """
        {"enfermedadId":"%s","medicacionId":"%s","observaciones":"Signos clínicos"}
        """
        .formatted(enfermedad, medicacion);
  }

  private String medicacionBody(
      UUID enfermedad, UUID medicamento, String nombre, String principio, String dias) {
    return """
        {"enfermedadId":"%s","medicamentoId":"%s","dosis":"200 g","duracionDias":5,
        "descripcion":"Agua de bebida","nombreMedicamento":"%s","principioActivo":"%s",
        "diasAplicacion":%s}
        """
        .formatted(enfermedad, medicamento, nombre, principio, dias);
  }

  private void inventarioDisponible(UUID medicamento, LocalDate hoy) {
    UUID recepcion = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO recepciones_medicamento (id, medicamento_id, codigo_lote, presentacion, fecha_ingreso, fecha_vencimiento, cantidad_presentaciones, contenido_por_presentacion, unidad_contenido, precio_neto_por_presentacion, impuesto_porcentaje, cantidad_unidad_base, unidad_base, precio_neto_unidad_base, precio_neto_total, valor_impuesto, precio_total, estado, actor, nombre_medicamento) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        recepcion,
        medicamento,
        "RX",
        "Frasco",
        hoy.minusDays(1),
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
        "Amprolio 20%");
    jdbc.update(
        "INSERT INTO movimientos_inventario (id, recepcion_id, producto_id, tipo_producto, tipo_movimiento, cantidad, unidad, actor, fecha_registro, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        UUID.randomUUID(),
        recepcion,
        medicamento,
        "MEDICAMENTO",
        "ENTRADA",
        100,
        "GRAMO",
        "admin",
        Instant.now(clock),
        "CONFIRMADO");
  }
}
