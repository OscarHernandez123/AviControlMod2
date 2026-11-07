package com.avicontrol.infrastructure.adapter.in.rest.mortalidad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avicontrol.AviControlModulo2Application;
import com.avicontrol.application.modulo1.ActualizarInventarioVivoCommand;
import com.avicontrol.application.modulo1.ActualizarInventarioVivoUseCase;
import com.avicontrol.domain.exception.modulo1.PoblacionVivaInsuficienteException;
import com.avicontrol.domain.exception.modulo1.ReferenciaLoteInconsistenteException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = AviControlModulo2Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MortalidadAlertaIntegrationTest {
  @DynamicPropertySource
  static void db(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.datasource.url",
        () ->
            "jdbc:h2:mem:mortalidad_alerta_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
  }

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired Clock clock;
  @Autowired ActualizarInventarioVivoUseCase inventario;

  @BeforeEach
  void limpiar() {
    jdbc.update("DELETE FROM alertas_sanitarias");
    jdbc.update("DELETE FROM registros_mortalidad_anormal");
    jdbc.update("DELETE FROM registros_mortalidad");
    jdbc.update("DELETE FROM descuentos_inventario_vivo");
    jdbc.update("DELETE FROM modulo1_galpon_local");
  }

  private SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor trabajador() {
    return SecurityMockMvcRequestPostProcessors.user("operario").roles("TRABAJADOR");
  }

  private SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor veterinario() {
    return SecurityMockMvcRequestPostProcessors.user("vet").roles("VETERINARIO");
  }

  private UUID galpon(int poblacion) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO modulo1_galpon_local (id,lote_id,estado,poblacion_viva,fecha_ingreso,nombre,aforo_maximo,lote_codigo) VALUES (?,?,?,?,?,?,?,?)",
        id,
        UUID.randomUUID(),
        "PRODUCTIVA",
        poblacion,
        LocalDate.now(clock).minusDays(40),
        "G1",
        10000,
        "L1");
    return id;
  }

  @Test
  void registraDescuentaYAlertaConRolesEIdempotencia() throws Exception {
    UUID normal = galpon(8000), anormal = galpon(5000), key = UUID.randomUUID();
    String fecha = OffsetDateTime.now(clock).minusMinutes(1).toString();
    String normalBody =
        "{\"fechaHoraEvento\":\""
            + fecha
            + "\",\"cantidad\":4,\"causa\":\"ENFERMEDAD\",\"observaciones\":\"Hallazgo diario\"}";
    mvc.perform(
            post("/api/galpones/{id}/mortalidad", normal)
                .contentType(MediaType.APPLICATION_JSON)
                .content(normalBody)
                .header("Idempotency-Key", key)
                .with(trabajador()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.clasificacion").value("NORMAL"))
        .andExpect(jsonPath("$.poblacionResultante").value(7996));
    mvc.perform(
            post("/api/galpones/{id}/mortalidad", normal)
                .contentType(MediaType.APPLICATION_JSON)
                .content(normalBody)
                .header("Idempotency-Key", key)
                .with(trabajador()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.poblacionResultante").value(7996));
    assertEquals(
        7996,
        jdbc.queryForObject(
            "SELECT poblacion_viva FROM modulo1_galpon_local WHERE id=?", Integer.class, normal));
    String anormalBody =
        "{\"fechaHoraEvento\":\""
            + fecha
            + "\",\"cantidad\":18,\"causaProbable\":\"RESPIRATORIA\",\"observaciones\":\"Aumento súbito\"}";
    UUID reporteKey = UUID.randomUUID();
    mvc.perform(
            post("/api/galpones/{id}/mortalidad-anormal", anormal)
                .contentType(MediaType.APPLICATION_JSON)
                .content(anormalBody)
                .header("Idempotency-Key", reporteKey)
                .with(trabajador()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.porcentajeMortalidad").value(0.36))
        .andExpect(jsonPath("$.severidad").value("ALTA"));
    mvc.perform(
            post("/api/galpones/{id}/mortalidad-anormal", anormal)
                .contentType(MediaType.APPLICATION_JSON)
                .content(anormalBody)
                .header("Idempotency-Key", reporteKey)
                .with(trabajador()))
        .andExpect(status().isCreated());
    mvc.perform(get("/api/alertas-sanitarias").with(veterinario()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElementos").value(1))
        .andExpect(jsonPath("$.elementos[0].cantidadMuertes").value(18));
    mvc.perform(get("/api/alertas-sanitarias").with(trabajador()))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/galpones/{id}/mortalidad", normal)
                .contentType(MediaType.APPLICATION_JSON)
                .content(normalBody))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void umbralDiarioAcumuladoGeneraUnaAlertaYPermiteDetalle() throws Exception {
    UUID galpon = galpon(5000);
    String fecha = OffsetDateTime.now(clock).minusMinutes(1).toString();
    String body = "{\"fechaHoraEvento\":\"" + fecha + "\",\"cantidad\":3,\"causa\":\"ENFERMEDAD\"}";
    mvc.perform(
            post("/api/galpones/{id}/mortalidad", galpon)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .header("Idempotency-Key", UUID.randomUUID())
                .with(trabajador()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.clasificacion").value("NORMAL"));
    mvc.perform(
            post("/api/galpones/{id}/mortalidad", galpon)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .header("Idempotency-Key", UUID.randomUUID())
                .with(trabajador()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.clasificacion").value("ANORMAL"));
    UUID alerta =
        jdbc.queryForObject(
            "SELECT id FROM alertas_sanitarias WHERE galpon_id=?", UUID.class, galpon);
    mvc.perform(get("/api/alertas-sanitarias/{id}", alerta).with(veterinario()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cantidadMuertes").value(3))
        .andExpect(jsonPath("$.nombreGalpon").value("G1"))
        .andExpect(jsonPath("$.poblacionActual").value(4994));
    assertEquals(
        4994,
        jdbc.queryForObject(
            "SELECT poblacion_viva FROM modulo1_galpon_local WHERE id=?", Integer.class, galpon));
  }

  @Test
  void causaAtipicaPermiteReporteBajoElUmbral() throws Exception {
    UUID galpon = galpon(5000);
    String fecha = OffsetDateTime.now(clock).minusMinutes(1).toString();
    String body =
        "{\"fechaHoraEvento\":\""
            + fecha
            + "\",\"cantidad\":1,\"causaProbable\":\"SINTOMAS_ATIPICOS\",\"observaciones\":\"Hallazgo sospechoso\"}";
    mvc.perform(
            post("/api/galpones/{id}/mortalidad-anormal", galpon)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(trabajador()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.porcentajeMortalidad").value(0.02))
        .andExpect(jsonPath("$.severidad").value("ALTA"));
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM alertas_sanitarias WHERE galpon_id=?", Integer.class, galpon));
    assertEquals(
        5000,
        jdbc.queryForObject(
            "SELECT poblacion_viva FROM modulo1_galpon_local WHERE id=?", Integer.class, galpon));
  }

  @Test
  void descuentoInternoEsIdempotenteYRespetaVersionYSaldo() {
    UUID galpon = galpon(50);
    UUID lote =
        jdbc.queryForObject(
            "SELECT lote_id FROM modulo1_galpon_local WHERE id=?", UUID.class, galpon);
    UUID clave = UUID.randomUUID();
    var comando =
        new ActualizarInventarioVivoCommand(lote, galpon, 25, "MORTALIDAD", "operario", 0, clave);
    assertEquals(25, inventario.actualizar(comando).poblacionResultante());
    assertEquals(25, inventario.actualizar(comando).poblacionResultante());
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM descuentos_inventario_vivo WHERE galpon_id=?",
            Integer.class,
            galpon));
    assertThrows(
        ReferenciaLoteInconsistenteException.class,
        () ->
            inventario.actualizar(
                new ActualizarInventarioVivoCommand(
                    lote, galpon, 1, "MORTALIDAD", "operario", 0, UUID.randomUUID())));
    assertThrows(
        PoblacionVivaInsuficienteException.class,
        () ->
            inventario.actualizar(
                new ActualizarInventarioVivoCommand(
                    lote, galpon, 26, "MORTALIDAD", "operario", -1, UUID.randomUUID())));
    assertEquals(
        25,
        jdbc.queryForObject(
            "SELECT poblacion_viva FROM modulo1_galpon_local WHERE id=?", Integer.class, galpon));
  }
}
